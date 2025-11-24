package com.agent.backend.dto

import com.agent.llm.message.LlmChatMessage
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.*

data class ChatMessageRequest(
    @field:NotBlank
    val content: String,
    val history: List<LlmChatMessage> = emptyList()
)

/**
 * Returned immediately by POST /chat/message.
 * For now status is "completed" and reply is the fixed string.
 * In the future, you'll return "queued" and let clients poll /chat/messages/{messageId}.
 */
data class ChatMessageResponse(
    val messageId: UUID,
    val userId: Long,
    val status: String,            // "queued" | "processing" | "completed" | "error"
    val echo: String,
    val reply: String?,            // assistant reply; null if still processing
    val queuedAt: Instant,
    val completedAt: Instant? = null,
    val delivery: DeliveryHint = DeliveryHint(mode = "poll", resultUrl = null)
)

data class DeliveryHint(
    /** "poll" | "sse" | "websocket" (future-friendly) */
    val mode: String,
    /** When mode = "poll", API path to retrieve the result */
    val resultUrl: String?
)

/** Returned by GET /chat/messages/{messageId} (polling). */
data class ChatMessageStatusResponse(
    val messageId: UUID,
    val userId: Long,
    val status: String,
    val reply: String?,
    val queuedAt: Instant,
    val completedAt: Instant?
)
