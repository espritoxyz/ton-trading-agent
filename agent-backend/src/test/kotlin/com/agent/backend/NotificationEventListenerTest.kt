package com.agent.backend

import com.agent.backend.db.entity.NotificationType
import com.agent.backend.rabbitmq.NotificationEvent
import com.agent.backend.rabbitmq.NotificationEventListener
import com.agent.backend.service.NotificationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

// Kotlin/Mockito null-safety helpers
private fun <T> any(type: Class<T>): T = ArgumentMatchers.any(type)
private fun anyLong(): Long = ArgumentMatchers.anyLong()
private fun anyString(): String = ArgumentMatchers.anyString()
private fun <K, V> anyMap(): Map<K, V> = ArgumentMatchers.anyMap()

class NotificationEventListenerTest {

    private lateinit var notificationService: NotificationService
    private lateinit var listener: NotificationEventListener

    @BeforeEach
    fun setup() {
        notificationService = mock(NotificationService::class.java)
        listener = NotificationEventListener(notificationService)
    }

    @Test
    fun `handleNotificationEvent should process valid event`() {
        // Given
        val event = NotificationEvent(
            userId = 1L,
            type = "BALANCE_CHANGE",
            title = "Balance Updated",
            message = "Your balance changed by 100 USD",
            metadata = mapOf("amount" to 100.0, "currency" to "USD"),
            timestamp = "2025-01-01T00:00:00Z"
        )

        // When
        listener.handleNotificationEvent(event)

        // Then
        verify(notificationService, times(1)).createNotification(
            anyLong(),
            any(NotificationType::class.java),
            anyString(),
            anyString(),
            anyMap()
        )
    }

    @Test
    fun `handleNotificationEvent should process TRANSACTION_COMPLETE event`() {
        // Given
        val event = NotificationEvent(
            userId = 2L,
            type = "TRANSACTION_COMPLETE",
            title = "Transaction Sent",
            message = "Successfully sent 5.0 TON",
            metadata = mapOf("transactionId" to "abc123", "status" to "success", "currency" to "TON"),
            timestamp = "2025-01-01T00:00:00Z"
        )

        // When
        listener.handleNotificationEvent(event)

        // Then
        verify(notificationService, times(1)).createNotification(
            anyLong(),
            any(NotificationType::class.java),
            anyString(),
            anyString(),
            anyMap()
        )
    }

    @Test
    fun `handleNotificationEvent should throw on invalid userId`() {
        // Given
        val event = NotificationEvent(
            userId = 0L,
            type = "BALANCE_CHANGE",
            title = "Test",
            message = "Test",
            metadata = emptyMap(),
            timestamp = "2025-01-01T00:00:00Z"
        )

        // When/Then
        assertThrows<IllegalArgumentException> {
            listener.handleNotificationEvent(event)
        }
    }

    @Test
    fun `handleNotificationEvent should throw on invalid notification type`() {
        // Given
        val event = NotificationEvent(
            userId = 1L,
            type = "INVALID_TYPE",
            title = "Test",
            message = "Test",
            metadata = emptyMap(),
            timestamp = "2025-01-01T00:00:00Z"
        )

        // When/Then
        assertThrows<IllegalArgumentException> {
            listener.handleNotificationEvent(event)
        }
    }

    @Test
    fun `handleNotificationEvent should throw on blank title`() {
        // Given
        val event = NotificationEvent(
            userId = 1L,
            type = "BALANCE_CHANGE",
            title = "",
            message = "Test",
            metadata = emptyMap(),
            timestamp = "2025-01-01T00:00:00Z"
        )

        // When/Then
        assertThrows<IllegalArgumentException> {
            listener.handleNotificationEvent(event)
        }
    }

    @Test
    fun `handleNotificationEvent should rethrow exception from notificationService`() {
        // Given
        val event = NotificationEvent(
            userId = 1L,
            type = "BALANCE_CHANGE",
            title = "Test",
            message = "Test",
            metadata = emptyMap(),
            timestamp = "2025-01-01T00:00:00Z"
        )
        `when`(
            notificationService.createNotification(
                anyLong(), any(NotificationType::class.java), anyString(), anyString(), anyMap()
            )
        ).thenThrow(RuntimeException("DB error"))

        // When/Then
        assertThrows<RuntimeException> {
            listener.handleNotificationEvent(event)
        }
    }

    @Test
    fun `handleNotificationEvent should process ORDER_FILLED event`() {
        // Given
        val event = NotificationEvent(
            userId = 3L,
            type = "ORDER_FILLED",
            title = "Order Filled",
            message = "Your buy order for 100 USDT at 5.0 has been filled",
            metadata = mapOf(
                "orderId" to "order-123",
                "symbol" to "USDT",
                "quantity" to 100,
                "price" to 5.0,
                "side" to "buy",
                "fillType" to "full"
            ),
            timestamp = "2025-01-01T00:00:00Z"
        )

        // When
        listener.handleNotificationEvent(event)

        // Then
        verify(notificationService, times(1)).createNotification(
            anyLong(),
            any(NotificationType::class.java),
            anyString(),
            anyString(),
            anyMap()
        )
    }

    @Test
    fun `handleNotificationEvent should process SWAP_EXECUTED event`() {
        // Given
        val event = NotificationEvent(
            userId = 4L,
            type = "SWAP_EXECUTED",
            title = "Swap Executed",
            message = "Swapped 10 TON for 50 USDT",
            metadata = mapOf(
                "fromAsset" to "TON",
                "toAsset" to "USDT",
                "fromAmount" to 10.0,
                "toAmount" to 50.0
            ),
            timestamp = "2025-01-01T00:00:00Z"
        )

        // When
        listener.handleNotificationEvent(event)

        // Then
        verify(notificationService, times(1)).createNotification(
            anyLong(),
            any(NotificationType::class.java),
            anyString(),
            anyString(),
            anyMap()
        )
    }
}
