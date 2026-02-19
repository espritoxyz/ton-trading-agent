package com.agent.backend

import com.agent.backend.controller.NotificationController
import com.agent.backend.db.entity.AgentUser
import com.agent.backend.db.entity.Notification
import com.agent.backend.db.entity.NotificationType
import com.agent.backend.dto.NotificationResponse
import com.agent.backend.service.NotificationService
import com.agent.backend.service.UserProvisioningService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant

// Helper functions to work around Mockito/Kotlin nullability issues
private fun <T> any(type: Class<T>): T = ArgumentMatchers.any(type)
private fun anyLong(): Long = ArgumentMatchers.anyLong()
private fun anyString(): String = ArgumentMatchers.anyString()

class NotificationControllerTest {

    private lateinit var notificationService: NotificationService
    private lateinit var provisioning: UserProvisioningService
    private lateinit var controller: NotificationController

    private lateinit var testUser: AgentUser
    private lateinit var testNotification: Notification
    private lateinit var testNotificationResponse: NotificationResponse
    private lateinit var authToken: JwtAuthenticationToken

    @BeforeEach
    fun setup() {
        notificationService = mock(NotificationService::class.java)
        provisioning = mock(UserProvisioningService::class.java)
        controller = NotificationController(notificationService, provisioning)

        // Setup test data
        testUser = AgentUser(
            id = 1L,
            subject = "test-subject",
            email = "test@example.com",
            emailVerified = true
        )

        testNotification = Notification(
            id = 1L,
            user = testUser,
            type = NotificationType.BALANCE_CHANGE,
            title = "Balance Updated",
            message = "Your balance changed",
            metadata = """{"amount": 100.0}""",
            isRead = false,
            createdAt = Instant.now()
        )

        testNotificationResponse = NotificationResponse(
            id = 1L,
            type = NotificationType.BALANCE_CHANGE,
            title = "Balance Updated",
            message = "Your balance changed",
            metadata = mapOf("amount" to 100.0),
            isRead = false,
            createdAt = testNotification.createdAt,
            readAt = null
        )
        `when`(notificationService.toResponse(any(Notification::class.java))).thenReturn(testNotificationResponse)

        // Mock JWT token
        val jwt = mock(Jwt::class.java)
        `when`(jwt.subject).thenReturn("test-subject")
        `when`(jwt.claims).thenReturn(mapOf("email" to "test@example.com"))
        authToken = JwtAuthenticationToken(jwt)

        // Mock user provisioning
        `when`(provisioning.resolveOrCreate(anyString(), anyString())).thenReturn(testUser)
    }

    @Test
    fun `getNotifications should return paginated notifications`() {
        // Given
        val page = PageImpl(listOf(testNotification), PageRequest.of(0, 20), 1)
        `when`(notificationService.getUserNotifications(anyLong(), any(Pageable::class.java)))
            .thenReturn(page)

        // When
        val response = controller.getNotifications(authToken, 0, 20, false)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(1, response.body!!.content.size)

        val notification = response.body!!.content[0]
        assertEquals(1L, notification.id)
        assertEquals(NotificationType.BALANCE_CHANGE, notification.type)
        assertFalse(notification.isRead)
    }

    @Test
    fun `getNotifications with unread filter should return only unread`() {
        // Given
        `when`(notificationService.getUnreadNotifications(1L))
            .thenReturn(listOf(testNotification))

        // When
        val response = controller.getNotifications(authToken, 0, 20, true)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(1, response.body!!.content.size)
        verify(notificationService).getUnreadNotifications(1L)
    }

    @Test
    fun `getUnreadCount should return correct count`() {
        // Given
        `when`(notificationService.getUnreadCount(1L)).thenReturn(5L)

        // When
        val response = controller.getUnreadCount(authToken)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(5L, response.body!!.count)
    }

    @Test
    fun `markAsRead should mark notification as read`() {
        // Given
        val readNotification = Notification(
            id = 1L,
            user = testUser,
            type = NotificationType.BALANCE_CHANGE,
            title = "Balance Updated",
            message = "Your balance changed",
            metadata = """{"amount": 100.0}""",
            isRead = true,
            createdAt = testNotification.createdAt,
            readAt = Instant.now()
        )
        `when`(notificationService.markAsRead(1L, 1L)).thenReturn(readNotification)
        val readResponse = testNotificationResponse.copy(isRead = true, readAt = readNotification.readAt)
        `when`(notificationService.toResponse(readNotification)).thenReturn(readResponse)

        // When
        val response = controller.markAsRead(authToken, 1L)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.isRead)
        assertNotNull(response.body!!.readAt)
    }

    @Test
    fun `markAsRead should return 404 when notification not found`() {
        // Given
        `when`(notificationService.markAsRead(999L, 1L))
            .thenThrow(NoSuchElementException("Notification not found"))

        // When
        val response = controller.markAsRead(authToken, 999L)

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `markAsRead should return 403 when user not authorized`() {
        // Given
        `when`(notificationService.markAsRead(1L, 1L))
            .thenThrow(IllegalArgumentException("Not authorized"))

        // When
        val response = controller.markAsRead(authToken, 1L)

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `deleteNotification should delete notification`() {
        // Given
        doNothing().`when`(notificationService).deleteNotification(1L, 1L)

        // When
        val response = controller.deleteNotification(authToken, 1L)

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        verify(notificationService).deleteNotification(1L, 1L)
    }

    @Test
    fun `deleteNotification should return 404 when notification not found`() {
        // Given
        doThrow(NoSuchElementException("Notification not found"))
            .`when`(notificationService).deleteNotification(999L, 1L)

        // When
        val response = controller.deleteNotification(authToken, 999L)

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `deleteNotification should return 403 when user not authorized`() {
        // Given
        doThrow(IllegalArgumentException("Not authorized"))
            .`when`(notificationService).deleteNotification(1L, 1L)

        // When
        val response = controller.deleteNotification(authToken, 1L)

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `notification response should contain all required fields`() {
        // Given
        val page = PageImpl(listOf(testNotification), PageRequest.of(0, 20), 1)
        `when`(notificationService.getUserNotifications(anyLong(), any(Pageable::class.java)))
            .thenReturn(page)

        // When
        val response = controller.getNotifications(authToken, 0, 20, false)

        // Then
        val notification = response.body!!.content[0]
        assertNotNull(notification.id)
        assertNotNull(notification.type)
        assertNotNull(notification.title)
        assertNotNull(notification.message)
        assertNotNull(notification.metadata)
        assertNotNull(notification.createdAt)
        assertFalse(notification.isRead)
        assertNull(notification.readAt)
    }
}
