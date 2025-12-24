package com.agent.backend.llm

import com.agent.backend.dto.ChatMessageRequest
import com.agent.backend.dto.ChatMessageResponse
import com.agent.backend.dto.ChatMessageStatusResponse
import com.agent.backend.dto.DeliveryHint
import com.agent.backend.service.ConfirmationItem
import com.agent.backend.service.ConfirmationService
import com.agent.llm.OpenAIChatter
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

enum class ChatJobStatus { QUEUED, PROCESSING, COMPLETED, ERROR }

data class ChatJob(
    val messageId: UUID,
    val userId: Long,
    val request: ChatMessageRequest,
    val queuedAt: Instant,
    @Volatile var status: ChatJobStatus = ChatJobStatus.QUEUED,
    @Volatile var reply: String? = null,
    @Volatile var completedAt: Instant? = null,
    // All planned tool calls (toolCallId, name, argsJson)
    @Volatile var plannedToolCalls: List<Triple<String, String, String>> = emptyList(),
    // Subset that do NOT require confirmation (toolCallId, name, argsJson)
    @Volatile var plannedNoConfirmCalls: List<Triple<String, String, String>> = emptyList()
)

@Service
class ChatJobService(
    private val rabbitTemplate: RabbitTemplate,
    private val confirmations: ConfirmationService
) {

    private val jobs = ConcurrentHashMap<UUID, ChatJob>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

    fun submit(userId: Long, body: ChatMessageRequest): ChatMessageResponse {
        val messageId = UUID.randomUUID()
        val now = Instant.now()
        val job = ChatJob(
            messageId = messageId,
            userId = userId,
            request = body,
            queuedAt = now
        )
        jobs[messageId] = job

        scope.launch {
            processJob(job)
        }

        return ChatMessageResponse(
            messageId = messageId,
            userId = userId,
            status = "queued",
            echo = body.content,
            reply = null,
            queuedAt = now,
            completedAt = null,
            delivery = DeliveryHint(
                mode = "poll",
                resultUrl = "/chat/messages/$messageId"
            )
        )
    }

    private fun makeChatter(job: ChatJob): OpenAIChatter =
        OpenAIChatter(
            chatHistory = job.request.history,
            bcAdapter = AgentBlockchainAdapter(job.userId, rabbitTemplate, job.messageId)
        )

    private suspend fun processJob(job: ChatJob) {
        try {
            job.status = ChatJobStatus.PROCESSING

            val chatter = makeChatter(job)

            val (planned, finalResponse) = chatter.planFirstStep(job.request.content)
            if (finalResponse != null) {
                job.reply = finalResponse.response
                job.completedAt = Instant.now()
                job.status = ChatJobStatus.COMPLETED
                return
            }

            // Store planned calls for future execution
            job.plannedToolCalls = planned.map { Triple(it.call.id, it.call.name, it.call.arguments) }
            job.plannedNoConfirmCalls = planned.filter { !it.requiresConfirmation }
                .map { Triple(it.call.id, it.call.name, it.call.arguments) }

            // Create confirmation items for those requiring confirmation
            planned.filter { it.requiresConfirmation }.forEach { p ->
                val item = ConfirmationItem(
                    messageId = job.messageId,
                    userId = job.userId,
                    toolCallId = p.call.id,
                    toolName = p.call.name,
                    argsJson = p.call.arguments,
                    text = p.confirmationText ?: "Please confirm executing ${p.call.name}"
                )
                confirmations.add(job.messageId, item)
            }

            if (planned.none { it.requiresConfirmation }) {
                // No confirmations required: execute all planned now
                val toolResponses = chatter.executeApprovedTools(job.plannedToolCalls)
                val resp = chatter.saveToolResponsesAndSummarize(toolResponses)
                job.reply = resp.response
                job.completedAt = Instant.now()
                job.status = ChatJobStatus.COMPLETED
            }
        } catch (e: Exception) {
            logger.error(e) {}
            job.reply = "Error while processing your request."
            job.completedAt = Instant.now()
            job.status = ChatJobStatus.ERROR
        }
    }

    fun resumeIfReady(messageId: UUID) {
        val job = jobs[messageId] ?: return
        if (!confirmations.allResolved(messageId)) return
        scope.launch {
            try {
                val chatter = makeChatter(job)
                val approvedTriples = confirmations.approved(messageId)
                    .map { Triple(it.toolCallId, it.toolName, it.argsJson) }
                // Execute both: non-confirmation planned calls + approved confirmation-required ones
                val toExecute = job.plannedNoConfirmCalls + approvedTriples
                val toolResponses = chatter.executeApprovedTools(toExecute)
                val hasAsyncTransfer = toolResponses.any { it.name == "send_ton_to_address" }
                if (hasAsyncTransfer) {
                    // Save tool response but DO NOT summarize or set reply; wait for finalizeWithToolResult
                    chatter.saveToolResponsesOnly(toolResponses)
                    job.status = ChatJobStatus.PROCESSING
                } else {
                    val resp = chatter.saveToolResponsesAndSummarize(toolResponses)
                    job.reply = resp.response
                    job.completedAt = Instant.now()
                    job.status = ChatJobStatus.COMPLETED
                }
            } catch (e: Exception) {
                logger.error(e) {}
                job.reply = "Error while processing your request."
                job.completedAt = Instant.now()
                job.status = ChatJobStatus.ERROR
            }
        }
    }

    suspend fun finalizeWithToolResult(messageId: UUID, userId: Long, toolName: String, toolResult: String) {
        val job = jobs[messageId] ?: return
        if (job.userId != userId) return
        logger.debug { "Finalizing request { userId: $userId, reply: $toolResult }" }
        // Temporary working fix: set the final reply directly without invoking LLM summarization.
        job.reply = toolResult
        job.completedAt = Instant.now()
        job.status = ChatJobStatus.COMPLETED
    }

    fun status(messageId: UUID, userId: Long): ChatMessageStatusResponse {
        val job = jobs[messageId] ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        if (job.userId != userId) throw ResponseStatusException(HttpStatus.FORBIDDEN)

        return ChatMessageStatusResponse(
            messageId = job.messageId,
            userId = job.userId,
            status = job.status.name.lowercase(), // "queued" | "processing" | ...
            reply = job.reply,
            queuedAt = job.queuedAt,
            completedAt = job.completedAt
        )
    }
}
