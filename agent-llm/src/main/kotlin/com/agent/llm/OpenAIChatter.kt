package com.agent.llm

import com.agent.llm.message.LlmChatMessage
import com.agent.llm.message.LlmChatMessageType
import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.ToolDefinitions
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.api.ConfirmationRequired
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
    private val bcAdapter: BlockchainAdapter
) {
    private val chatEnv: ChatEnvironment
    private val router = AiRouterLocal()
    private val modelConfig: ModelConfig
    private val allTools = ToolDefinitions(bcAdapter).allTools

    init {
        logger.debug { "Initializing OpenAIChatter. historySize=${chatHistory.size} tools=${allTools.map { it.definition.name }}" }
        val allModels = router.availableModels().providerToModelConfigs
        val openAiModels = allModels[RemoteProvider.OpenAI] ?: emptyList()
        logger.debug { "Available OpenAI models: ${openAiModels.map { it.modelName }}" }
        val modelInfo = openAiModels.first { it.modelName.contains("gpt-5-mini") }
        logger.debug { "Chosen model: ${modelInfo.modelName}" }
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
        logger.debug { "ChatEnvironment initialized with ${historyMessages.size} messages" }
    }

    data class PlannedToolCall(
        val call: ToolCall,
        val requiresConfirmation: Boolean,
        val confirmationText: String? = null
    )

    suspend fun planFirstStep(userRequestContent: String): Pair<List<PlannedToolCall>, ChatResponse?> {
        logger.debug { "Received user request: ${userRequestContent.take(200)}" }
        if (chatHistory.isEmpty()) {
            logger.debug { "No prior history, injecting system message" }
            val systemMessage = AgentPrompt.makeAgentMessage(bcAdapter)
            chatEnv.saveMessage(systemMessage)
        }

        val userMessage = Message.user(userRequestContent)
        chatEnv.saveMessage(userMessage)
        val prompt = Prompt(messages = chatEnv.chatHistory)
        logger.debug { "Calling router.chat with prompt messages=${prompt.messages.size}" }
        val response = router.chat(ChatRequest(modelConfig, prompt))
        logger.debug { "LLM response received: toolCalls=${response.toolCalls.size}" }
        if (response.toolCalls.isEmpty()) {
            logger.debug { "No tool calls planned by the model" }
            return Pair(emptyList(), response)
        }
        val planned = response.toolCalls.map { tc ->
            val tool = AgentTool.fromToolCall(allTools, tc)
            val needs = tool is ConfirmationRequired
            val text = if (needs) (tool as ConfirmationRequired).confirmationText(tc.arguments) else null
            logger.debug { "Planned tool: name=${tc.name} requiresConfirmation=$needs args=${tc.arguments}" }
            PlannedToolCall(tc, needs, text)
        }
        // Save assistant tool call message
        val assistantMessage = Message.assistant("", toolCalls = response.toolCalls)
        chatEnv.saveMessage(assistantMessage)
        return planned to null
    }

    @Suppress("UNCHECKED_CAST")
    fun executeApprovedTools(approved: List<Triple<String, String, String>>): List<ToolResponse> {
        logger.debug { "Executing approved tools: count=${approved.size} -> ${approved.map { it.second }}" }
        return approved.map { (toolCallId, name, argsJson) ->
            val agentTool = allTools.firstOrNull { it.definition.name == name }
                ?: error("No agent tool named $name found")
            val anyTool = agentTool as AgentTool<Any?>
            val args = Json.decodeFromString(anyTool.argsSerializer, argsJson)
            val stringRes = anyTool.payload(args)
            logger.debug { "Tool executed: name=$name resultPreview='${stringRes.take(200)}'" }
            ToolResponse(toolCallId, name, stringRes)
        }
    }

    fun saveToolResponsesOnly(toolResponses: List<ToolResponse>) {
        logger.debug { "Saving tool responses only: count=${toolResponses.size}" }
        val toolMessage = Message.tool(toolResponses)
        chatEnv.saveMessage(toolMessage)
    }

    suspend fun saveToolResponsesAndSummarize(toolResponses: List<ToolResponse>): ChatResponse {
        logger.debug { "Saving tool responses and summarizing: count=${toolResponses.size}" }
        val toolMessage = Message.tool(toolResponses)
        chatEnv.saveMessage(toolMessage)
        val prompt = Prompt(chatEnv.chatHistory)
        val summary = router.chat(ChatRequest(modelConfig, prompt))
        logger.debug { "Summary received: toolCalls=${summary.toolCalls.size}" }
        return summary
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun executeApprovedToolsAndSummarize(approved: List<Triple<String, String, String>>): ChatResponse {
        logger.debug { "executeApprovedToolsAndSummarize invoked with ${approved.size} approvals" }
        val toolResponses = executeApprovedTools(approved)
        return saveToolResponsesAndSummarize(toolResponses)
    }
}
