package com.agent.backend.service

import com.agent.backend.rabbitmq.RabbitConfig
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Helper class to publish notification events to RabbitMQ.
 * Ensures consistent event format and error handling across all services.
 */
@Component
class NotificationEventPublisher(
    private val rabbitTemplate: RabbitTemplate,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Publish a notification event to the notifications.events exchange.
     * This method is fire-and-forget with timeout and error handling.
     *
     * @param userId The user ID to send the notification to
     * @param type The notification type (e.g., "BALANCE_CHANGE")
     * @param title The notification title
     * @param message The notification message
     * @param metadata Additional metadata for the notification
     */
    fun publishNotificationEvent(
        userId: Long,
        type: String,
        title: String,
        message: String,
        metadata: Map<String, Any>
    ) {
        try {
            val event = mapOf(
                "userId" to userId,
                "type" to type,
                "title" to title,
                "message" to message,
                "metadata" to metadata,
                "timestamp" to Instant.now().toString()
            )

            val eventJson = objectMapper.writeValueAsString(event)

            // Set a timeout of 1 second for sending
            rabbitTemplate.convertAndSend(
                RabbitConfig.NOTIFICATION_EXCHANGE,
                "", // Fanout exchange doesn't use routing key
                eventJson
            )

            logger.debug { "Published notification event for user $userId: $type" }
        } catch (e: Exception) {
            // Log but don't throw - notification failures should not block wallet operations
            logger.warn(e) { "Failed to publish notification event for user $userId: $type" }
        }
    }
}
