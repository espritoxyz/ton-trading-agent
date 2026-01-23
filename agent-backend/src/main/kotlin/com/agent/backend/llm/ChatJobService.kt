package com.agent.backend.llm

import com.agent.backend.dto.ChatMessageRequest
import com.agent.backend.dto.ChatMessageResponse
import com.agent.backend.dto.ChatMessageStatusResponse
import com.agent.backend.dto.DeliveryHint
import com.agent.backend.service.*
import com.agent.llm.ChatterStatus
import com.agent.llm.OpenAIChatter
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

data class ChatJob(
    val messageId: UUID,
    val userId: Long,
    val request: ChatMessageRequest,
    val queuedAt: Instant,
    var status: AtomicReference<ChatterStatus> = AtomicReference(ChatterStatus.PROCESSING),
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
    private val confirmations: ConfirmationService,
    private val poolsCacheService: StonfiPoolsCacheService,
    private val assetsCache: StonfiAssetsCacheService,
) {

    private val jobs = ConcurrentHashMap<UUID, ChatJob>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Map of confirmationId -> deferred result (true=approved, false=declined)
    private val pendingConfirmations = ConcurrentHashMap<UUID, CompletableDeferred<Boolean>>()

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
            bcAdapter = AgentBlockchainAdapter(
                job.userId,
                rabbitTemplate,
                job.messageId,
                poolsCacheService,
                assetsCache
            )
        )

    private suspend fun processJob(job: ChatJob) {
        try {
            val chatter = makeChatter(job)
            job.status = chatter.atomicStatus
            val (stringResponse, isPreGenAnswer) = chatter.processRequest(
                messageId = job.messageId,
                userRequestContent = job.request.content,
                requestConfirmation = { msgId, plannedTc ->
                    val tc = plannedTc.call
                    val item = ConfirmationItem(
                        messageId = msgId,
                        toolName = tc.name,
                        text = plannedTc.confirmationText ?: "Please confirm executing ${tc.name} with ${tc.arguments}",
                        argsJson = tc.arguments
                    )

                    confirmations.add(job.messageId, item)
                    val deferred = CompletableDeferred<Boolean>()
                    pendingConfirmations[item.id] = deferred
                    logger.debug { "Waiting for confirmation ${item.id} on message $msgId for tool ${tc.name}" }
                    deferred.await().also {
                        logger.debug { "Confirmation awaited for ${item.id}" }
                    }
                }
            )

            if (!isPreGenAnswer && stringResponse != null) {
                job.completedAt = Instant.now()
                job.status = AtomicReference(ChatterStatus.COMPLETED)
                job.reply = stringResponse
            }
        } catch (e: Exception) {
            logger.error(e) {}
            job.reply = "Error while processing your request."
            job.completedAt = Instant.now()
            job.status = AtomicReference(ChatterStatus.ERROR)
        }
    }

    suspend fun finalizeWithToolResult(messageId: UUID, userId: Long, toolName: String, toolResult: String) {
        val job = jobs[messageId] ?: return
        if (job.userId != userId) return
        logger.debug { "Finalizing request for $toolName { userId: $userId, reply: $toolResult }" }
        // Temporary working fix: set the final reply directly without invoking LLM summarization.
        job.reply = toolResult
        job.completedAt = Instant.now()
        job.status = AtomicReference(ChatterStatus.COMPLETED)
    }

    // Called by ConfirmationController after approve/decline
    fun resumeIfReady(messageId: UUID) {
        val items = confirmations.list(messageId)
        items.forEach { item ->
            if (item.status != ConfirmationStatus.PENDING) {
                val approved = item.status == ConfirmationStatus.APPROVED
                pendingConfirmations.remove(item.id)?.complete(approved)
            }
        }
    }

    fun status(messageId: UUID, userId: Long): ChatMessageStatusResponse {
        val job = jobs[messageId] ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        if (job.userId != userId) throw ResponseStatusException(HttpStatus.FORBIDDEN)

        return ChatMessageStatusResponse(
            messageId = job.messageId,
            userId = job.userId,
            status = job.status.get().name.lowercase(), // "queued" | "processing" | ...
            reply = job.reply,
            queuedAt = job.queuedAt,
            completedAt = job.completedAt
        )
    }
}
