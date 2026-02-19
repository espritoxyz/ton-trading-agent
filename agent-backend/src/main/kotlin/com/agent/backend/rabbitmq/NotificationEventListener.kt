package com.agent.backend.rabbitmq

import com.agent.backend.db.entity.NotificationType
import com.agent.backend.service.NotificationService
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

data class NotificationEvent(
    val userId: Long,
    val type: String,
    val title: String,
    val message: String,
    val metadata: Map<String, Any>,
    val timestamp: String
)

@Component
class NotificationEventListener(
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    @RabbitListener(queues = [RabbitConfig.NOTIFICATION_QUEUE])
    fun handleNotificationEvent(message: String) {
        try {
            logger.info { "Received notification event: $message" }

            val event = objectMapper.readValue(message, NotificationEvent::class.java)

            // Validate required fields
            require(event.userId > 0) { "Invalid userId: ${event.userId}" }
            require(event.type.isNotBlank()) { "Notification type is required" }
            require(event.title.isNotBlank()) { "Notification title is required" }
            require(event.message.isNotBlank()) { "Notification message is required" }

            // Parse notification type
            val notificationType = try {
                NotificationType.valueOf(event.type)
            } catch (e: IllegalArgumentException) {
                logger.error { "Invalid notification type: ${event.type}" }
                throw IllegalArgumentException("Invalid notification type: ${event.type}", e)
            }

            // Create notification (will also broadcast via WebSocket)
            val notification = notificationService.createNotification(
                userId = event.userId,
                type = notificationType,
                title = event.title,
                message = event.message,
                metadata = event.metadata
            )

            logger.info { "Successfully created notification ${notification.id} for user ${event.userId}" }

        } catch (e: Exception) {
            logger.error(e) { "Failed to process notification event: $message" }
            // Exception will cause message to be requeued or sent to DLQ based on retry policy
            throw e
        }
    }
}
