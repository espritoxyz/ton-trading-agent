package com.agent.backend

import com.agent.backend.rabbitmq.RabbitConfig
import com.agent.backend.service.NotificationEventPublisher
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.amqp.rabbit.core.RabbitTemplate

class NotificationEventPublisherTest {

    private lateinit var rabbitTemplate: RabbitTemplate
    private lateinit var objectMapper: ObjectMapper
    private lateinit var publisher: NotificationEventPublisher

    @BeforeEach
    fun setup() {
        rabbitTemplate = mock(RabbitTemplate::class.java)
        objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        publisher = NotificationEventPublisher(rabbitTemplate, objectMapper)
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
            anyString()
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

        // Capture the message sent
        var capturedMessage: String? = null
        doAnswer { invocation ->
            capturedMessage = invocation.getArgument(2) as String
            null
        }.`when`(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString())

        // When
        publisher.publishNotificationEvent(userId, type, title, message, metadata)

        // Then
        assertNotNull(capturedMessage)
        val eventMap = objectMapper.readValue(capturedMessage, Map::class.java)

        assertEquals(2L, (eventMap["userId"] as Number).toLong())
        assertEquals("TRANSACTION_COMPLETE", eventMap["type"])
        assertEquals("Transaction Sent", eventMap["title"])
        assertEquals("Successfully sent 5.0 TON", eventMap["message"])
        assertNotNull(eventMap["metadata"])
        assertNotNull(eventMap["timestamp"])
    }

    @Test
    fun `publishNotificationEvent should not throw when RabbitMQ fails`() {
        // Given
        doThrow(RuntimeException("RabbitMQ connection failed"))
            .`when`(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString())

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

        var capturedMessage: String? = null
        doAnswer { invocation ->
            capturedMessage = invocation.getArgument(2) as String
            null
        }.`when`(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString())

        // When
        publisher.publishNotificationEvent(userId, type, title, message, metadata)

        // Then
        assertNotNull(capturedMessage)
        val eventMap = objectMapper.readValue(capturedMessage, Map::class.java)
        assertTrue((eventMap["metadata"] as Map<*, *>).isEmpty())
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

        var capturedMessage: String? = null
        doAnswer { invocation ->
            capturedMessage = invocation.getArgument(2) as String
            null
        }.`when`(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString())

        // When
        publisher.publishNotificationEvent(userId, type, title, message, metadata)

        // Then
        assertNotNull(capturedMessage)
        val eventMap = objectMapper.readValue(capturedMessage, Map::class.java)
        val eventMetadata = eventMap["metadata"] as Map<*, *>

        assertEquals("swap-123", eventMetadata["swapId"])
        assertEquals("TON", eventMetadata["fromAsset"])
        assertNotNull(eventMetadata["nestedData"])
    }
}
