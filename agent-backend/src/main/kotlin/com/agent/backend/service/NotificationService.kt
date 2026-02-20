package com.agent.backend.service

import com.agent.backend.db.entity.Notification
import com.agent.backend.db.entity.NotificationType
import com.agent.backend.db.rep.AgentUserRepository
import com.agent.backend.db.rep.NotificationRepository
import com.agent.backend.dto.NotificationResponse
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
            metadata = metadata
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
            val response = NotificationResponse.from(notification)
            messagingTemplate.convertAndSend("/topic/notifications/$userId", response)
            logger.info { "Broadcasted notification ${notification.id} to user $userId via WebSocket" }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to broadcast notification ${notification.id} to user $userId via WebSocket" }
            // Don't throw - WebSocket broadcast failure should not break notification creation
        }
    }

    /**
     * Send a wallet-refresh signal to the user's WebSocket topic without saving anything to the DB.
     * The frontend treats this as a prompt to reload wallet state (assets, orders, transactions).
     */
    fun broadcastWalletRefresh(userId: Long) {
        try {
            val signal = NotificationResponse(
                id = -1L,
                type = NotificationType.BALANCE_CHANGE,
                title = "",
                message = "",
                metadata = emptyMap(),
                isRead = true,
                createdAt = java.time.Instant.now(),
                readAt = null,
                refreshOnly = true
            )
            messagingTemplate.convertAndSend("/topic/notifications/$userId", signal)
            logger.debug { "Sent wallet-refresh signal to user $userId via WebSocket" }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to send wallet-refresh signal to user $userId" }
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

    fun toResponse(notification: Notification): NotificationResponse {
        return NotificationResponse.from(notification)
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
                Pair(
                    "Balance Updated",
                    "Your $currency balance has changed by $amount"
                )
            }

            NotificationType.TRANSACTION_COMPLETE -> {
                val status = metadata["status"] as? String
                val amount = metadata["amount"]
                val currency = metadata["currency"] ?: "TON"
                val message = when (status) {
                    "success" -> "Successfully sent $amount $currency"
                    "failed" -> "Transaction failed: ${metadata["errorReason"] ?: "Unknown error"}"
                    else -> "Transaction completed"
                }
                Pair("Transaction Complete", message)
            }

            NotificationType.SWAP_EXECUTED -> {
                val fromAmount = metadata["fromAmount"]
                val fromAsset = metadata["fromAsset"]
                val toAmount = metadata["toAmount"]
                val toAsset = metadata["toAsset"]
                Pair(
                    "Swap Executed",
                    "Swapped $fromAmount $fromAsset for $toAmount $toAsset"
                )
            }

            NotificationType.ORDER_FILLED -> {
                val status = metadata["status"] as? String
                if (status == "cancelled") {
                    val orderId = metadata["orderId"]
                    Pair(
                        "Order Cancelled",
                        "Order #$orderId was cancelled"
                    )
                } else {
                    val side = metadata["side"] ?: "buy"
                    val quantity = metadata["quantity"] ?: metadata["filledQuantity"]
                    val symbol = metadata["symbol"]
                    val price = metadata["price"]
                    Pair(
                        "Order Conditions Met",
                        "Target price $price reached for your $side order of $quantity $symbol. Initiating swap..."
                    )
                }
            }

            NotificationType.TRACKER_TRIGGERED -> {
                val symbol = metadata["symbol"]
                val price = metadata["targetPrice"]
                val direction = metadata["direction"]
                val directionText = if (direction == "UP") "risen to" else "fallen to"
                Pair(
                    "Price Alert Triggered",
                    "$symbol has $directionText $price"
                )
            }
        }
    }
}
