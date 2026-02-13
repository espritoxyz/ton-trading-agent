package com.agent.backend.dto

import com.agent.backend.db.entity.Notification
import com.agent.backend.db.entity.NotificationType
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Instant

data class NotificationResponse(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val message: String,
    val metadata: Map<String, Any>,
    val isRead: Boolean,
    val createdAt: Instant,
    val readAt: Instant?
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(notification: Notification, objectMapper: ObjectMapper): NotificationResponse {
            val metadataMap = try {
                objectMapper.readValue(notification.metadata, Map::class.java) as? Map<String, Any> ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            return NotificationResponse(
                id = notification.id!!,
                type = notification.type,
                title = notification.title,
                message = notification.message,
                metadata = metadataMap,
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
