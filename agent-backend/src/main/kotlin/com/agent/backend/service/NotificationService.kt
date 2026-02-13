package com.agent.backend.service

import com.agent.backend.db.entity.Notification
import com.agent.backend.db.entity.NotificationType
import com.agent.backend.db.rep.AgentUserRepository
import com.agent.backend.db.rep.NotificationRepository
import com.agent.backend.dto.NotificationResponse
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: AgentUserRepository,
    private val objectMapper: ObjectMapper,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    fun createNotification(
        userId: Long,
        type: NotificationType,
        title: String,
        message: String,
        metadata: Map<String, Any>
    ): Notification {
        val user = userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("User not found: $userId") }

        val notification = Notification(
            user = user,
            type = type,
            title = title,
            message = message,
            metadata = objectMapper.writeValueAsString(metadata)
        )

        val savedNotification = notificationRepository.save(notification)

        // Broadcast to WebSocket subscribers
        broadcastNotificationToUser(userId, savedNotification)

        return savedNotification
    }

    /**
     * Broadcast notification to user via WebSocket.
     * Fails silently if user is not connected or WebSocket is unavailable.
     */
    private fun broadcastNotificationToUser(userId: Long, notification: Notification) {
        try {
            val response = NotificationResponse.from(notification, objectMapper)
            messagingTemplate.convertAndSend("/topic/notifications/$userId", response)
            logger.info { "Broadcasted notification ${notification.id} to user $userId via WebSocket" }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to broadcast notification ${notification.id} to user $userId via WebSocket" }
            // Don't throw - WebSocket broadcast failure should not break notification creation
        }
    }

    fun getUserNotifications(userId: Long, pageable: Pageable): Page<Notification> {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable)
    }

    fun getUnreadNotifications(userId: Long): List<Notification> {
        return notificationRepository.findByUser_IdAndIsReadFalse(userId)
    }

    fun getUnreadCount(userId: Long): Long {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId)
    }

    @Transactional
    fun markAsRead(notificationId: Long, userId: Long): Notification {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { NoSuchElementException("Notification not found: $notificationId") }

        // Authorization check
        if (notification.user.id != userId) {
            throw IllegalArgumentException("User $userId is not authorized to access notification $notificationId")
        }

        if (!notification.isRead) {
            notification.isRead = true
            notification.readAt = java.time.Instant.now()
            return notificationRepository.save(notification)
        }

        return notification
    }

    @Transactional
    fun deleteNotification(notificationId: Long, userId: Long) {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { NoSuchElementException("Notification not found: $notificationId") }

        // Authorization check
        if (notification.user.id != userId) {
            throw IllegalArgumentException("User $userId is not authorized to delete notification $notificationId")
        }

        notificationRepository.delete(notification)
    }

    @Transactional
    fun deleteAllNotifications(userId: Long): Int {
        val deletedCount = notificationRepository.deleteByUser_Id(userId)
        logger.info { "Deleted $deletedCount notifications for user $userId" }
        return deletedCount
    }

    @Transactional
    fun markAllAsRead(userId: Long): Int {
        val now = java.time.Instant.now()
        val updatedCount = notificationRepository.markAllAsReadByUserId(userId, now)
        logger.info { "Marked $updatedCount notifications as read for user $userId" }
        return updatedCount
    }

    /**
     * Generate human-readable notification title and message based on type and metadata.
     * Returns Pair<title, message>
     */
    fun generateNotificationText(type: NotificationType, metadata: Map<String, Any>): Pair<String, String> {
        return when (type) {
            NotificationType.BALANCE_CHANGE -> {
                val amount = metadata["amount"] ?: "unknown"
                val currency = metadata["currency"] ?: "TON"
                val title = "Balance Updated"
                val message = "Your $currency balance changed by $amount"
                Pair(title, message)
            }
            NotificationType.TRANSACTION_COMPLETE -> {
                val status = metadata["status"] as? String
                val amount = metadata["amount"]
                val currency = metadata["currency"] ?: "TON"
                val title = "Transaction Complete"
                val message = when (status) {
                    "success" -> "Successfully sent $amount $currency"
                    "failed" -> "Transaction failed: ${metadata["errorReason"] ?: "Unknown error"}"
                    else -> "Transaction completed"
                }
                Pair(title, message)
            }
            NotificationType.SWAP_EXECUTED -> {
                val fromAmount = metadata["fromAmount"]
                val fromAsset = metadata["fromAsset"]
                val toAmount = metadata["toAmount"]
                val toAsset = metadata["toAsset"]
                val title = "Swap Executed"
                val message = "Swapped $fromAmount $fromAsset for $toAmount $toAsset"
                Pair(title, message)
            }
            NotificationType.ORDER_FILLED -> {
                val status = metadata["status"] as? String
                if (status == "cancelled") {
                    val orderId = metadata["orderId"]
                    val title = "Order Cancelled"
                    val message = "Order #$orderId was cancelled"
                    Pair(title, message)
                } else {
                    val side = metadata["side"] ?: "buy"
                    val quantity = metadata["quantity"] ?: metadata["filledQuantity"]
                    val symbol = metadata["symbol"]
                    val price = metadata["price"]
                    val fillType = metadata["fillType"] ?: "full"
                    val title = "Order Filled"
                    val message = "Your $side order for $quantity $symbol at $price has been $fillType filled"
                    Pair(title, message)
                }
            }
        }
    }
}
