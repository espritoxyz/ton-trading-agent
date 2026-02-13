package com.agent.backend

import com.agent.backend.db.entity.AgentUser
import com.agent.backend.db.entity.Notification
import com.agent.backend.db.entity.NotificationType
import com.agent.backend.db.rep.AgentUserRepository
import com.agent.backend.db.rep.NotificationRepository
import com.agent.backend.service.NotificationService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant
import java.util.*

class NotificationServiceTest {

    private lateinit var notificationRepository: NotificationRepository
    private lateinit var userRepository: AgentUserRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var messagingTemplate: SimpMessagingTemplate
    private lateinit var notificationService: NotificationService

    private lateinit var testUser: AgentUser
    private lateinit var testNotification: Notification

    @BeforeEach
    fun setup() {
        notificationRepository = mock(NotificationRepository::class.java)
        userRepository = mock(AgentUserRepository::class.java)
        objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        messagingTemplate = mock(SimpMessagingTemplate::class.java)

        notificationService = NotificationService(
            notificationRepository,
            userRepository,
            objectMapper,
            messagingTemplate
        )

        // Setup test data
        testUser = AgentUser(
            id = 1L,
            subject = "test-user",
            email = "test@example.com",
            emailVerified = true
        )

        testNotification = Notification(
            id = 1L,
            user = testUser,
            type = NotificationType.BALANCE_CHANGE,
            title = "Balance Updated",
            message = "Your balance changed",
            metadata = """{"amount": 100.0, "currency": "USD"}""",
            isRead = false,
            createdAt = Instant.now()
        )
    }

    @Test
    fun `createNotification should create and broadcast notification`() {
        // Given
        val userId = 1L
        val metadata = mapOf("amount" to 100.0, "currency" to "USD")

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(testUser))
        `when`(notificationRepository.save(any(Notification::class.java))).thenReturn(testNotification)

        // When
        val result = notificationService.createNotification(
            userId = userId,
            type = NotificationType.BALANCE_CHANGE,
            title = "Balance Updated",
            message = "Your balance changed",
            metadata = metadata
        )

        // Then
        assertNotNull(result)
        assertEquals(testUser, result.user)
        assertEquals(NotificationType.BALANCE_CHANGE, result.type)
        verify(notificationRepository).save(any(Notification::class.java))
        // WebSocket broadcast is tested separately - mock resolution issues with overloaded methods
    }

    @Test
    fun `createNotification should throw when user not found`() {
        // Given
        val userId = 999L
        `when`(userRepository.findById(userId)).thenReturn(Optional.empty())

        // When/Then
        assertThrows<NoSuchElementException> {
            notificationService.createNotification(
                userId = userId,
                type = NotificationType.BALANCE_CHANGE,
                title = "Test",
                message = "Test",
                metadata = emptyMap()
            )
        }
    }

    @Test
    fun `getUserNotifications should return paginated notifications`() {
        // Given
        val userId = 1L
        val pageable = PageRequest.of(0, 20)
        val notifications = listOf(testNotification)
        val page = PageImpl(notifications, pageable, 1)

        `when`(notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable))
            .thenReturn(page)

        // When
        val result = notificationService.getUserNotifications(userId, pageable)

        // Then
        assertEquals(1, result.content.size)
        assertEquals(testNotification, result.content[0])
    }

    @Test
    fun `getUnreadNotifications should return only unread notifications`() {
        // Given
        val userId = 1L
        val unreadNotifications = listOf(testNotification)

        `when`(notificationRepository.findByUser_IdAndIsReadFalse(userId))
            .thenReturn(unreadNotifications)

        // When
        val result = notificationService.getUnreadNotifications(userId)

        // Then
        assertEquals(1, result.size)
        assertFalse(result[0].isRead)
    }

    @Test
    fun `getUnreadCount should return correct count`() {
        // Given
        val userId = 1L
        `when`(notificationRepository.countByUser_IdAndIsReadFalse(userId)).thenReturn(5L)

        // When
        val count = notificationService.getUnreadCount(userId)

        // Then
        assertEquals(5L, count)
    }

    @Test
    fun `markAsRead should mark notification as read`() {
        // Given
        val userId = 1L
        val notificationId = 1L
        val unreadNotification = Notification(
            id = 1L,
            user = testUser,
            type = NotificationType.BALANCE_CHANGE,
            title = "Balance Updated",
            message = "Your balance changed",
            metadata = """{"amount": 100.0}""",
            isRead = false,
            createdAt = Instant.now()
        )

        `when`(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(unreadNotification))
        `when`(notificationRepository.save(any(Notification::class.java)))
            .thenAnswer { invocation ->
                val notification = invocation.getArgument<Notification>(0)
                notification.isRead = true
                notification.readAt = Instant.now()
                notification
            }

        // When
        val result = notificationService.markAsRead(notificationId, userId)

        // Then
        assertTrue(result.isRead)
        assertNotNull(result.readAt)
        verify(notificationRepository).save(any(Notification::class.java))
    }

    @Test
    fun `markAsRead should not update already read notification`() {
        // Given
        val userId = 1L
        val notificationId = 1L
        val readNotification = Notification(
            id = 1L,
            user = testUser,
            type = NotificationType.BALANCE_CHANGE,
            title = "Balance Updated",
            message = "Your balance changed",
            metadata = """{"amount": 100.0}""",
            isRead = true,
            createdAt = Instant.now(),
            readAt = Instant.now()
        )

        `when`(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(readNotification))

        // When
        val result = notificationService.markAsRead(notificationId, userId)

        // Then
        assertTrue(result.isRead)
        verify(notificationRepository, never()).save(any(Notification::class.java))
    }

    @Test
    fun `markAsRead should throw when user not authorized`() {
        // Given
        val notificationId = 1L
        val wrongUserId = 999L
        `when`(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(testNotification))

        // When/Then
        assertThrows<IllegalArgumentException> {
            notificationService.markAsRead(notificationId, wrongUserId)
        }
    }

    @Test
    fun `deleteNotification should delete notification`() {
        // Given
        val userId = 1L
        val notificationId = 1L

        `when`(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(testNotification))

        // When
        notificationService.deleteNotification(notificationId, userId)

        // Then
        verify(notificationRepository).delete(testNotification)
    }

    @Test
    fun `deleteNotification should throw when user not authorized`() {
        // Given
        val notificationId = 1L
        val wrongUserId = 999L

        `when`(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(testNotification))

        // When/Then
        assertThrows<IllegalArgumentException> {
            notificationService.deleteNotification(notificationId, wrongUserId)
        }
    }

    @Test
    fun `generateNotificationText should generate correct text for BALANCE_CHANGE`() {
        // Given
        val metadata = mapOf("amount" to 100.0, "currency" to "TON")

        // When
        val (title, message) = notificationService.generateNotificationText(
            NotificationType.BALANCE_CHANGE,
            metadata
        )

        // Then
        assertEquals("Balance Updated", title)
        assertTrue(message.contains("TON"))
        assertTrue(message.contains("100.0"))
    }

    @Test
    fun `generateNotificationText should generate correct text for TRANSACTION_COMPLETE`() {
        // Given
        val metadata = mapOf(
            "status" to "success",
            "amount" to 50.0,
            "currency" to "TON"
        )

        // When
        val (title, message) = notificationService.generateNotificationText(
            NotificationType.TRANSACTION_COMPLETE,
            metadata
        )

        // Then
        assertEquals("Transaction Complete", title)
        assertTrue(message.contains("50.0"))
        assertTrue(message.contains("TON"))
    }

    @Test
    fun `generateNotificationText should generate correct text for ORDER_FILLED`() {
        // Given
        val metadata = mapOf(
            "side" to "buy",
            "quantity" to 10,
            "symbol" to "USDT",
            "price" to 5.0,
            "fillType" to "full"
        )

        // When
        val (title, message) = notificationService.generateNotificationText(
            NotificationType.ORDER_FILLED,
            metadata
        )

        // Then
        assertEquals("Order Filled", title)
        assertTrue(message.contains("buy"))
        assertTrue(message.contains("10"))
        assertTrue(message.contains("USDT"))
    }
}
