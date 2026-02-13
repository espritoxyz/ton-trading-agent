package com.agent.backend

import com.agent.backend.db.entity.NotificationType
import com.agent.backend.rabbitmq.NotificationEventListener
import com.agent.backend.service.NotificationService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*

// Helper functions to work around Mockito/Kotlin nullability issues
private fun <T> any(type: Class<T>): T = ArgumentMatchers.any(type)
private fun anyLong(): Long = ArgumentMatchers.anyLong()
private fun anyString(): String = ArgumentMatchers.anyString()
private fun <K, V> anyMap(): Map<K, V> = ArgumentMatchers.anyMap()

class NotificationEventListenerTest {

    private lateinit var notificationService: NotificationService
    private lateinit var objectMapper: ObjectMapper
    private lateinit var listener: NotificationEventListener

    @BeforeEach
    fun setup() {
        notificationService = mock(NotificationService::class.java)
        objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        listener = NotificationEventListener(notificationService, objectMapper)
    }

    @Test
    fun `handleNotificationEvent should process valid event`() {
        // Given
        val eventJson = """
            {
                "userId": 1,
                "type": "BALANCE_CHANGE",
                "title": "Balance Updated",
                "message": "Your balance changed by 100 USD",
                "metadata": {
                    "amount": 100.0,
                    "currency": "USD"
                },
                "timestamp": "2025-01-01T00:00:00Z"
            }
        """.trimIndent()

        // When
        listener.handleNotificationEvent(eventJson)

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
        val eventJson = """
            {
                "userId": 2,
                "type": "TRANSACTION_COMPLETE",
                "title": "Transaction Sent",
                "message": "Successfully sent 5.0 TON",
                "metadata": {
                    "transactionId": "abc123",
                    "status": "success",
                    "amount": 5000000000,
                    "currency": "TON"
                },
                "timestamp": "2025-01-01T00:00:00Z"
            }
        """.trimIndent()

        // When
        listener.handleNotificationEvent(eventJson)

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
        val eventJson = """
            {
                "userId": 0,
                "type": "BALANCE_CHANGE",
                "title": "Test",
                "message": "Test",
                "metadata": {},
                "timestamp": "2025-01-01T00:00:00Z"
            }
        """.trimIndent()

        // When/Then
        assertThrows<IllegalArgumentException> {
            listener.handleNotificationEvent(eventJson)
        }
    }

    @Test
    fun `handleNotificationEvent should throw on invalid notification type`() {
        // Given
        val eventJson = """
            {
                "userId": 1,
                "type": "INVALID_TYPE",
                "title": "Test",
                "message": "Test",
                "metadata": {},
                "timestamp": "2025-01-01T00:00:00Z"
            }
        """.trimIndent()

        // When/Then
        assertThrows<IllegalArgumentException> {
            listener.handleNotificationEvent(eventJson)
        }
    }

    @Test
    fun `handleNotificationEvent should throw on missing required fields`() {
        // Given - missing title
        val eventJson = """
            {
                "userId": 1,
                "type": "BALANCE_CHANGE",
                "message": "Test",
                "metadata": {},
                "timestamp": "2025-01-01T00:00:00Z"
            }
        """.trimIndent()

        // When/Then
        assertThrows<Exception> {
            listener.handleNotificationEvent(eventJson)
        }
    }

    @Test
    fun `handleNotificationEvent should throw on malformed JSON`() {
        // Given
        val malformedJson = "{ invalid json }"

        // When/Then
        assertThrows<Exception> {
            listener.handleNotificationEvent(malformedJson)
        }
    }

    @Test
    fun `handleNotificationEvent should process ORDER_FILLED event`() {
        // Given
        val eventJson = """
            {
                "userId": 3,
                "type": "ORDER_FILLED",
                "title": "Order Filled",
                "message": "Your buy order for 100 USDT at 5.0 has been filled",
                "metadata": {
                    "orderId": "order-123",
                    "symbol": "USDT",
                    "quantity": 100,
                    "price": 5.0,
                    "side": "buy",
                    "fillType": "full"
                },
                "timestamp": "2025-01-01T00:00:00Z"
            }
        """.trimIndent()

        // When
        listener.handleNotificationEvent(eventJson)

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
        val eventJson = """
            {
                "userId": 4,
                "type": "SWAP_EXECUTED",
                "title": "Swap Executed",
                "message": "Swapped 10 TON for 50 USDT",
                "metadata": {
                    "swapId": "swap-456",
                    "fromAsset": "TON",
                    "toAsset": "USDT",
                    "fromAmount": 10.0,
                    "toAmount": 50.0,
                    "executionPrice": 5.0
                },
                "timestamp": "2025-01-01T00:00:00Z"
            }
        """.trimIndent()

        // When
        listener.handleNotificationEvent(eventJson)

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
