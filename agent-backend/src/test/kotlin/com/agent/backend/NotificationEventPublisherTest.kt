package com.agent.backend

import com.agent.backend.rabbitmq.RabbitConfig
import com.agent.backend.service.NotificationEventPublisher
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.springframework.amqp.rabbit.core.RabbitTemplate

// Kotlin/Mockito null-safety helpers
private fun anyString(): String = ArgumentMatchers.anyString()
private fun <K, V> anyMap(): Map<K, V> = ArgumentMatchers.anyMap()

class NotificationEventPublisherTest {

    private lateinit var rabbitTemplate: RabbitTemplate
    private lateinit var publisher: NotificationEventPublisher

    @BeforeEach
    fun setup() {
        rabbitTemplate = mock(RabbitTemplate::class.java)
        publisher = NotificationEventPublisher(rabbitTemplate)
    }

    @Test
    fun `publishNotificationEvent should send message to correct exchange`() {
        // Given
        val userId = 1L
        val type = "BALANCE_CHANGE"
        val title = "Balance Updated"
        val message = "Your balance changed"
        val metadata = mapOf("amount" to 100.0, "currency" to "USD")

        // When
        publisher.publishNotificationEvent(userId, type, title, message, metadata)

        // Then
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitConfig.NOTIFICATION_EXCHANGE),
            eq(""),
            anyMap<String, Any>()
        )
    }

    @Test
    fun `publishNotificationEvent should include all required fields in message`() {
        // Given
        val userId = 2L
        val type = "TRANSACTION_COMPLETE"
        val title = "Transaction Sent"
        val message = "Successfully sent 5.0 TON"
        val metadata = mapOf(
            "transactionId" to "abc123",
            "status" to "success",
            "amount" to 5000000000L,
            "currency" to "TON"
        )

        // Capture the map sent to rabbitTemplate
        var capturedMap: Map<*, *>? = null
        doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedMap = invocation.getArgument(2) as Map<*, *>
            null
        }.`when`(rabbitTemplate).convertAndSend(anyString(), anyString(), anyMap<String, Any>())

        // When
        publisher.publishNotificationEvent(userId, type, title, message, metadata)

        // Then
        assertNotNull(capturedMap)
        assertEquals(2L, (capturedMap!!["userId"] as Number).toLong())
        assertEquals("TRANSACTION_COMPLETE", capturedMap!!["type"])
        assertEquals("Transaction Sent", capturedMap!!["title"])
        assertEquals("Successfully sent 5.0 TON", capturedMap!!["message"])
        assertNotNull(capturedMap!!["metadata"])
        assertNotNull(capturedMap!!["timestamp"])
    }

    @Test
    fun `publishNotificationEvent should not throw when RabbitMQ fails`() {
        // Given
        doThrow(RuntimeException("RabbitMQ connection failed"))
            .`when`(rabbitTemplate).convertAndSend(anyString(), anyString(), anyMap<String, Any>())

        // When/Then - should not throw
        assertDoesNotThrow {
            publisher.publishNotificationEvent(
                userId = 1L,
                type = "BALANCE_CHANGE",
                title = "Test",
                message = "Test",
                metadata = emptyMap()
            )
        }
    }

    @Test
    fun `publishNotificationEvent should handle empty metadata`() {
        // Given
        val userId = 3L
        val type = "ORDER_FILLED"
        val title = "Order Filled"
        val message = "Your order has been filled"
        val metadata = emptyMap<String, Any>()

        var capturedMap: Map<*, *>? = null
        doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedMap = invocation.getArgument(2) as Map<*, *>
            null
        }.`when`(rabbitTemplate).convertAndSend(anyString(), anyString(), anyMap<String, Any>())

        // When
        publisher.publishNotificationEvent(userId, type, title, message, metadata)

        // Then
        assertNotNull(capturedMap)
        assertTrue((capturedMap!!["metadata"] as Map<*, *>).isEmpty())
    }

    @Test
    fun `publishNotificationEvent should handle complex metadata`() {
        // Given
        val userId = 4L
        val type = "SWAP_EXECUTED"
        val title = "Swap Executed"
        val message = "Swap completed successfully"
        val metadata = mapOf(
            "swapId" to "swap-123",
            "fromAsset" to "TON",
            "toAsset" to "USDT",
            "fromAmount" to 10.0,
            "toAmount" to 50.0,
            "executionPrice" to 5.0,
            "slippagePercent" to 0.5,
            "nestedData" to mapOf(
                "poolAddress" to "EQAbc123...",
                "fee" to 0.003
            )
        )

        var capturedMap: Map<*, *>? = null
        doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedMap = invocation.getArgument(2) as Map<*, *>
            null
        }.`when`(rabbitTemplate).convertAndSend(anyString(), anyString(), anyMap<String, Any>())

        // When
        publisher.publishNotificationEvent(userId, type, title, message, metadata)

        // Then
        assertNotNull(capturedMap)
        val eventMetadata = capturedMap!!["metadata"] as Map<*, *>
        assertEquals("swap-123", eventMetadata["swapId"])
        assertEquals("TON", eventMetadata["fromAsset"])
        assertNotNull(eventMetadata["nestedData"])
    }
}
