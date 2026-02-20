package com.agent.backend.dto

import com.agent.backend.db.entity.Notification
import com.agent.backend.db.entity.NotificationType
import java.time.Instant

data class NotificationResponse(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val message: String,
    val metadata: Map<String, Any>,
    val isRead: Boolean,
    val createdAt: Instant,
    val readAt: Instant?,
    /** When true, this message is a UI-refresh signal only and must not be displayed to the user. */
    val refreshOnly: Boolean = false
) {
    companion object {
        fun from(notification: Notification): NotificationResponse {
            return NotificationResponse(
                id = notification.id!!,
                type = notification.type,
                title = notification.title,
                message = notification.message,
                metadata = notification.metadata,
                isRead = notification.isRead,
                createdAt = notification.createdAt,
                readAt = notification.readAt
            )
        }
    }
}

data class UnreadCountResponse(
    val count: Long
)
