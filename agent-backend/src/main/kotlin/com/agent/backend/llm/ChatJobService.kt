package com.agent.backend.llm

import com.agent.backend.dto.ChatMessageRequest
import com.agent.backend.dto.ChatMessageResponse
import com.agent.backend.dto.ChatMessageStatusResponse
import com.agent.backend.dto.DeliveryHint
import com.agent.llm.OpenAIChatter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import jakarta.annotation.PreDestroy
import kotlin.math.log
import kotlinx.coroutines.*

private val logger = KotlinLogging.logger {}

enum class ChatJobStatus { QUEUED, PROCESSING, COMPLETED, ERROR }

data class ChatJob(
    val messageId: UUID,
    val userId: Long,
    val request: ChatMessageRequest,
    val queuedAt: Instant,
    @Volatile var status: ChatJobStatus = ChatJobStatus.QUEUED,
    @Volatile var reply: String? = null,
    @Volatile var completedAt: Instant? = null
)

@Service
class ChatJobService {

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

    private suspend fun processJob(job: ChatJob) {
        try {
            job.status = ChatJobStatus.PROCESSING

            val chatter = OpenAIChatter(
                chatHistory = job.request.history,
                bcAdapter = AgentBlockchainAdapter(job.userId)
            )

            val reply = chatter.sendUserRequest(job.request.content)
            job.reply = reply
            job.completedAt = Instant.now()
            job.status = ChatJobStatus.COMPLETED
        } catch (e: Exception) {
            logger.error(e) {}
            job.reply = "Error while processing your request."
            job.completedAt = Instant.now()
            job.status = ChatJobStatus.ERROR
        }
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
