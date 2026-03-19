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

/** A single confirmation item sent inside ChatUpdateEvent. */
data class ConfirmationUpdate(
    val id: String,
    val toolName: String,
    val text: String,
    val status: String   // "PENDING" | "APPROVED" | "DECLINED"
)

/**
 * Pushed via WebSocket to /topic/chat/{userId} whenever job state changes:
 *  - toolcalling  : a new confirmation was requested
 *  - processing   : all confirmations resolved, resuming
 *  - completed    : final reply is ready
 *  - error        : processing failed
 */
data class ChatUpdateEvent(
    val messageId: UUID,
    val userId: Long,
    val status: String,
    val reply: String?,
    val confirmations: List<ConfirmationUpdate> = emptyList()
)
