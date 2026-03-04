package com.agent.llm

import com.agent.llm.message.LlmChatMessage
import com.agent.llm.message.LlmChatMessageType
import com.agent.llm.tool.ToolDefinitions
import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.api.ConfirmationRequired
import com.explyt.ai.backend.http.ApiKeyParam
import com.explyt.ai.dto.ChatRequest
import com.explyt.ai.dto.ChatResponse
import com.explyt.ai.dto.Message
import com.explyt.ai.dto.MessageType
import com.explyt.ai.dto.ModelConfig
import com.explyt.ai.dto.Prompt
import com.explyt.ai.dto.ToolCall
import com.explyt.ai.dto.ToolResponse
import com.explyt.ai.router.dto.RemoteProvider
import com.explyt.ai.router.router.AiRouterLocal
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

class ConfirmationDeclinedException : Exception("Confirmation declined by user")

class OpenAIChatter(
    private val chatHistory: List<LlmChatMessage>,
    private val bcAdapter: BlockchainAdapter
) {
    private val chatEnv: ChatEnvironment
    private val router = AiRouterLocal()
    private val modelConfig: ModelConfig
    private val allTools = ToolDefinitions(bcAdapter).allTools
    private val maxDepth = 20
    private val confirmationReqNames = allTools.filter { it is ConfirmationRequired }.map { it.definition.name }.toSet()

    val atomicStatus = AtomicReference(ChatterStatus.PROCESSING)

    val messageHistory: List<Message>
        get() = chatEnv.chatHistory

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

    data class RequestAnswer(
        val responseString: String?,
    )

    suspend fun processRequest(
        messageId: UUID,
        userRequestContent: String,
        // Returns true or false based on UI confirmation boxes
        requestConfirmation: suspend (UUID, PlannedToolCall) -> Boolean
    ): RequestAnswer {
        logger.debug { "Received user request: ${userRequestContent.take(200)}" }
        bcAdapter.updateCurrentMessageId(messageId)
        var currentMessage = Message.user(userRequestContent)
        var chatResponse: ChatResponse? = null
        var inc = 0

        @Suppress("UNCHECKED_CAST")
        suspend fun callTools(plannedToolCalls: List<PlannedToolCall>): List<ToolResponse> {
            atomicStatus.set(ChatterStatus.TOOLCALLING)
            val approvedPlanned = coroutineScope {
                plannedToolCalls.map { plannedTc ->
                    async {
                        val needs = plannedTc.call.name in confirmationReqNames
                        val approved = if (needs) {
                            requestConfirmation(messageId, plannedTc)
                        } else {
                            true
                        }

                        plannedTc to approved
                    }
                }.awaitAll()
            }.filter { it.second }.map { it.first }

            logger.debug {
                "Approved tool calls: ${approvedPlanned.size}/${plannedToolCalls.size} -> ${approvedPlanned.map { it.call.name }}"
            }

            if (approvedPlanned.isEmpty()) throw ConfirmationDeclinedException()
            
            return approvedPlanned.map { plannedToolCall ->
                val tc = plannedToolCall.call

                val agentTool = allTools.firstOrNull { it.definition.name == tc.name }
                    ?: error("No agent tool named ${tc.name} found")

                val anyTool = agentTool as AgentTool<Any?>
                val args = Json.decodeFromString(anyTool.argsSerializer, tc.arguments)
                val stringRes = anyTool.payload(args)
                logger.debug { "Tool executed: name=${tc.name} resultPreview='${stringRes.take(200)}'" }
                ToolResponse(tc.id, tc.name, stringRes)
            }
        }

        do {
            logger.debug { "Current message: ${currentMessage.type}(${currentMessage.content})" }
            if (currentMessage.type == MessageType.USER || currentMessage.type == MessageType.TOOL) {
                val loopResponse = callLoop(currentMessage)
                chatResponse = loopResponse.chatResponse
                val plannedTcs = loopResponse.plannedToolCalls
                if (plannedTcs.isNotEmpty()) {
                    val toolResponses = callTools(plannedTcs)
                    currentMessage = Message.tool(toolResponses)
                } else {
                    val assistantMessage = Message.assistant("")
                    currentMessage = assistantMessage
                    // Explicit save here for "stitching" llm histories between user requests
                    chatEnv.saveMessage(assistantMessage)
                }
            } else if (currentMessage.type == MessageType.ASSISTANT) {
                if (currentMessage.toolCalls.isEmpty()) {
                    logger.debug { "Finished request processing for messageId=$messageId " }
                    return RequestAnswer(
                        chatResponse?.response,
                    )
                }
                error("Unreachable state with ${currentMessage.toolCalls.size} tools in assistant message")
            }
            inc++
        } while (chatResponse != null && inc < maxDepth)

        if (inc == maxDepth) {
            logger.error { "Max depth of $maxDepth was achieved on messageId=$messageId" }
        }

        return RequestAnswer(
            chatResponse?.response,
        )

    }

    private suspend fun callLoop(message: Message): LlmResponse {
        if (chatEnv.chatHistory.isEmpty()) {
            logger.debug { "No prior history in ChatEnvironment, injecting initial system message" }
            val systemMessage = AgentPrompt.makeAgentMessage(bcAdapter)
            chatEnv.saveMessage(systemMessage)
        }
        chatEnv.saveMessage(message)

        when (message.type) {
            MessageType.USER -> {
                logger.debug { "Received user message: ${message.content}" }
                val prompt = Prompt(messages = chatEnv.chatHistory)
                val response = runCatching {
                    logger.debug { "Calling router.chat with prompt messages=${prompt.messages.size}" }
                    atomicStatus.set(ChatterStatus.PROCESSING)
                    router.chat(ChatRequest(modelConfig, prompt))
                }.getOrElse { e ->
                    logger.error(e) { "LLM chat failed with history error, wiping and continuing" }
                    chatEnv.clearHistory()
                    return callLoop(message)
                }
                return llmResponse(response, response.toolCalls)
            }

            MessageType.TOOL -> {
                val toolResponses = message.toolResponses
                logger.debug { "LLM tool messages received: count=${toolResponses.size} -> ${toolResponses.map { it.name }}" }

                val prompt = Prompt(chatEnv.chatHistory)
                atomicStatus.set(ChatterStatus.PROCESSING)
                var response = router.chat(ChatRequest(modelConfig, prompt))

                // No further tool calls = summarize executed
                if (response.toolCalls.isEmpty() && toolResponses.any { it.responseData.isNotBlank() }) {
                    response = summarizeToolCalls(toolResponses)
                }

                return llmResponse(response, response.toolCalls)
            }

            else -> {
                error("Unknown message type ${message.type}")
            }
        }
    }

    private suspend fun summarizeToolCalls(toolResponses: List<ToolResponse>): ChatResponse {
        val enriched = bcAdapter.awaitExternalResults(toolResponses)
        val requestContent = buildString {
            appendLine(AgentPrompt.utilitySummarizeAnchor)
            enriched.forEach {
                appendLine(it.responseData)
            }
        }

        val assistantMessage = Message.assistant("")
        val utilityRequest = Message.user(requestContent)
        val prompt = Prompt(chatEnv.chatHistory + assistantMessage + utilityRequest)
        return router.chat(ChatRequest(modelConfig, prompt))
    }


    private fun llmResponse(
        response: ChatResponse?,
        toolCalls: List<ToolCall>
    ): LlmResponse {
        logger.debug { "LLM response received: toolCalls=${toolCalls.size}" }
        if (toolCalls.isEmpty()) {
            logger.debug { "No tool calls planned by the model" }
            return LlmResponse(response, emptyList())
        }

        val assistantMessage = Message.assistant("", toolCalls = toolCalls)
        chatEnv.saveMessage(assistantMessage)

        return LlmResponse(response, planFromToolCalls(toolCalls))
    }

    private fun planFromToolCalls(toolCalls: List<ToolCall>): List<PlannedToolCall> =
        toolCalls.map { tc ->
            val tool = AgentTool.fromToolCall(allTools, tc)
            val needs = tool is ConfirmationRequired
            val text = if (needs) (tool as ConfirmationRequired).confirmationText(tc.arguments) else null
            logger.debug { "Planned tool: name=${tc.name} requiresConfirmation=$needs args=${tc.arguments}" }
            PlannedToolCall(tc, needs, text)
        }

    data class LlmResponse(
        val chatResponse: ChatResponse? = null,
        val plannedToolCalls: List<PlannedToolCall>,
    )
}
