package com.agent.backend.llm

import com.agent.backend.AppUtils
import com.agent.backend.dto.ChatMessageRequest
import com.agent.backend.dto.ChatMessageResponse
import com.agent.backend.dto.ChatMessageStatusResponse
import com.agent.backend.dto.DeliveryHint
import com.agent.backend.service.ConfirmationItem
import com.agent.backend.service.ConfirmationService
import com.agent.backend.service.ConfirmationStatus
import com.agent.backend.service.ExternalToolResultService
import com.agent.backend.service.NotificationService
import com.agent.backend.service.OrderService
import com.agent.backend.service.PriceTrackerService
import com.agent.backend.service.StonfiAssetsCacheService
import com.agent.backend.service.StonfiPoolsCacheService
import com.agent.backend.service.WalletService
import com.agent.llm.ChatterStatus
import com.agent.llm.OpenAIChatter
import com.agent.llm.ConfirmationDeclinedException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CompletableDeferred
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
    private val priceTrackerService: PriceTrackerService,
    private val orderService: OrderService,
    private val externalToolResultService: ExternalToolResultService,
    private val walletService: WalletService,
    private val notificationService: NotificationService,
    private val appUtils: AppUtils,
) {

    private val jobs = ConcurrentHashMap<UUID, ChatJob>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Map of confirmationId -> deferred result (true=approved, false=declined)
    private val pendingConfirmations = ConcurrentHashMap<UUID, CompletableDeferred<Boolean>>()

    // Cache of chatters to preserve history between user requests
    private val userIdToChatter = ConcurrentHashMap<Long, OpenAIChatter>()

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

    private fun makeChatter(job: ChatJob, previousChatter: OpenAIChatter?): OpenAIChatter =
        if (previousChatter != null && job.request.history.isNotEmpty()) {
            logger.debug { "Returning existing chatter for userId ${job.userId} with ${previousChatter.messageHistory.size} messages" }
            previousChatter
        } else {
            logger.debug { "Returning new chatter for userId ${job.userId}" }
            val chatter = makeChatter(job)
            userIdToChatter[job.userId] = chatter
            chatter
        }

    private fun makeChatter(job: ChatJob): OpenAIChatter =
        OpenAIChatter(
            chatHistory = job.request.history,
            bcAdapter = AgentBlockchainAdapter(
                job.userId,
                rabbitTemplate,
                job.messageId,
                poolsCacheService,
                assetsCache,
                walletService,
                priceTrackerService,
                orderService,
                externalToolResultService,
                notificationService,
                appUtils
            )
        )

    private suspend fun processJob(job: ChatJob) {
        if (jobs.values.count { it.userId == job.userId && !it.status.get().isFinished } > 1) {
            logger.warn { "User ${job.userId} has in-progress job" }
            job.completedAt = Instant.now()
            job.status = AtomicReference(ChatterStatus.ERROR)
            job.reply = "You currently have another processing request."
        }
        try {
            val chatter = makeChatter(job, userIdToChatter[job.userId])
            job.status = chatter.atomicStatus
            val (stringResponse) = chatter.processRequest(
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

            job.completedAt = Instant.now()
            job.status = AtomicReference(ChatterStatus.COMPLETED)
            job.reply = stringResponse
        } catch (e: ConfirmationDeclinedException) {
            logger.debug { "Confirmation declined for messageId=${job.messageId}" }
            job.reply = "Confirmation declined by user"
            job.completedAt = Instant.now()
            job.status = AtomicReference(ChatterStatus.COMPLETED)
        } catch (e: Exception) {
            logger.error(e) {}
            job.reply = "Error while processing your request."
            job.completedAt = Instant.now()
            job.status = AtomicReference(ChatterStatus.ERROR)
        }
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

        if (job.status.get().isFinished) {
            jobs.remove(messageId)
        }

        return ChatMessageStatusResponse(
            messageId = job.messageId,
            userId = job.userId,
            status = job.status.get().name.lowercase(),
            reply = job.reply,
            queuedAt = job.queuedAt,
            completedAt = job.completedAt
        )
    }
}
