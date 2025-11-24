package com.agent.llm

import com.agent.llm.message.LlmChatMessage
import com.agent.llm.message.LlmChatMessageType
import com.agent.llm.tool.AgentTool
import com.agent.llm.tool.ToolDefinitions
import com.explyt.ai.backend.http.ApiKeyParam
import com.explyt.ai.dto.ChatRequest
import com.explyt.ai.dto.ChatResponse
import com.explyt.ai.dto.Message
import com.explyt.ai.dto.ModelConfig
import com.explyt.ai.dto.Prompt
import com.explyt.ai.dto.ToolCall
import com.explyt.ai.dto.ToolResponse
import com.explyt.ai.router.dto.RemoteProvider
import com.explyt.ai.router.router.AiRouterLocal
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

class OpenAIChatter(
    private val chatHistory: List<LlmChatMessage>,
    bcAdapter: BlockchainAdapter
) {
    private val chatEnv: ChatEnvironment
    private val router = AiRouterLocal()
    private val modelConfig: ModelConfig
    private val allTools = ToolDefinitions(bcAdapter).allTools

    init {
        val allModels = router.availableModels().providerToModelConfigs
        val openAiModels = allModels[RemoteProvider.OpenAI] ?: emptyList()
        val modelInfo = openAiModels.first { it.modelName.contains("gpt-5-mini") }
        modelConfig = ModelConfig(
            modelInfo = modelInfo,
            modelSpecificParams = ApiKeyParam(System.getenv("OPENAI_API_KEY")),
            toolDefinitions = allTools.map { it.definition }
        )
        val historyMessages = chatHistory.map {
            when (it.type) {
                LlmChatMessageType.USER -> Message.user(it.content)
                // POSSIBLE BUGS BECAUSE OF POOR LLM HISTORY DESERIALIZATION
                // TODO: examine behaviour
                LlmChatMessageType.SYSTEM -> Message.assistant(it.content)
            }
        }
        chatEnv = ChatEnvironment(historyMessages)
    }

    suspend fun sendUserRequest(userRequestContent: String): String {
        logger.debug { "Received user request: ${userRequestContent.take(30)}"  }
        val userMessage = Message.user(userRequestContent)
        chatEnv.saveMessage(userMessage)
        val systemMessage = AgentPrompt.makeAgentMessage()
        val prompt = Prompt(messages = listOf(systemMessage) + chatEnv.chatHistory)
        var response = router.chat(ChatRequest(modelConfig, prompt))
        while (response.toolCalls.isNotEmpty()) {
            val assistantMessage = Message.assistant("", toolCalls = response.toolCalls)
            chatEnv.saveMessage(assistantMessage)
            response = processToolcall(response)
        }

        return response.response
    }

    private suspend fun processToolcall(response: ChatResponse): ChatResponse {
        val toolResponses = response.toolCalls.map { toolCall ->
            val agentTool = AgentTool.fromToolCall(allTools, toolCall)
                ?: error("No agent tool matching $toolCall found")
            val stringRes = callTool(agentTool, toolCall)
            ToolResponse(toolCall.id, toolCall.name, stringRes)
        }

        logger.debug { "Processing ${toolResponses.size} tools: ${toolResponses.joinToString { it.name }}" }
        val toolMessage = Message.tool(toolResponses)
        chatEnv.saveMessage(toolMessage)
        return router.chat(ChatRequest(modelConfig, Prompt(chatEnv.chatHistory)))
    }

    private fun <A> callTool(agentTool: AgentTool<A>, toolCall: ToolCall): String {
        val args = Json.decodeFromString(agentTool.argsSerializer, toolCall.arguments)
        logger.debug { "Calling ${agentTool.definition.name} with ${toolCall.arguments}" }
        return agentTool.payload(args)
    }
}
