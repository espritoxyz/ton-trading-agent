package com.agent.backend

import com.agent.backend.controller.NewsletterController
import com.agent.backend.dto.NewsletterBroadcastRequest
import com.agent.backend.dto.NewsletterResendRequest
import com.agent.backend.dto.NewsletterSubscribeRequest
import com.agent.backend.dto.NewsletterSubscriptionUpdateRequest
import com.agent.backend.dto.NewsletterUnsubscribeRequest
import com.agent.backend.service.ConfirmResult
import com.agent.backend.service.NewsletterService
import com.agent.backend.service.ResendResult
import com.agent.backend.service.SubscribeResult
import com.agent.backend.service.UnsubscribeResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class NewsletterControllerTest {

    private lateinit var newsletterService: NewsletterService
    private lateinit var controller: NewsletterController

    @BeforeEach
    fun setup() {
        newsletterService = mock(NewsletterService::class.java)
        controller = NewsletterController(newsletterService)
    }

    private fun jwtAuth(email: String?, roles: List<String> = emptyList()): JwtAuthenticationToken {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.subject).thenReturn("test-subject")

        val realmAccess: Map<String, Any> = mapOf("roles" to roles)
        val claims = mutableMapOf<String, Any>("realm_access" to realmAccess)
        if (email != null) claims["email"] = email

        `when`(jwt.claims).thenReturn(claims)
        return JwtAuthenticationToken(jwt)
    }

    @Test
    fun `subscribe endpoint should map subscribed result`() {
        // Given
        val email = "user@example.com"
        `when`(newsletterService.subscribe(email)).thenReturn(SubscribeResult.SUBSCRIBED)

        // When
        val response = controller.subscribe(NewsletterSubscribeRequest(email))

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.pending)
        assertTrue(response.body!!.message.isNotBlank())
        verify(newsletterService).subscribe(email)
    }

    @Test
    fun `confirm endpoint should map invalid token`() {
        // Given
        `when`(newsletterService.confirmSubscription("bad")).thenReturn(ConfirmResult.INVALID_TOKEN)

        // When
        val response = controller.confirm("bad")

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(false, response.body!!.confirmed)
    }

    @Test
    fun `resend verification endpoint should be vague on not found`() {
        // Given
        val email = "x@example.com"
        `when`(newsletterService.resendVerification(email)).thenReturn(ResendResult.NOT_FOUND)

        // When
        val response = controller.resendVerification(NewsletterResendRequest(email))

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(true, response.body!!.sent)
    }

    @Test
    fun `unsubscribe endpoint should be vague on not found`() {
        // Given
        val email = "x@example.com"
        `when`(newsletterService.unsubscribeByEmail(email)).thenReturn(UnsubscribeResult.NOT_FOUND)

        // When
        val response = controller.unsubscribeByEmail(NewsletterUnsubscribeRequest(email))

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(true, response.body!!.unsubscribed)
    }

    @Test
    fun `getMySubscription should return 401 when no auth`() {
        val response = controller.getMySubscription(null)
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `getMySubscription should return 400 when email claim missing`() {
        val response = controller.getMySubscription(jwtAuth(email = null))
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `getMySubscription should return subscription status`() {
        // Given
        val email = "me@example.com"
        `when`(newsletterService.getNewsletterStatus(email)).thenReturn(true)

        // When
        val response = controller.getMySubscription(jwtAuth(email))

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(true, response.body!!.subscribed)
    }

    @Test
    fun `updateMySubscription should return 401 when no auth`() {
        val response = controller.updateMySubscription(null, NewsletterSubscriptionUpdateRequest(subscribed = true))
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `admin preview should return 403 when not admin`() {
        val response = controller.previewNewsletter(
            jwtAuth(email = "a@example.com", roles = listOf("USER")),
            NewsletterBroadcastRequest(subject = "s", htmlContent = "c")
        )
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `admin send should return 200 when admin`() {
        // Given
        val auth = jwtAuth(email = "a@example.com", roles = listOf("ADMIN"))
        `when`(newsletterService.broadcast("s", "c")).thenReturn(
            com.agent.backend.dto.NewsletterBroadcastResponse(totalSubscribers = 1, sent = 1, failed = 0)
        )

        // When
        val response = controller.sendNewsletter(auth, NewsletterBroadcastRequest(subject = "s", htmlContent = "c"))

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(1, response.body!!.sent)
        verify(newsletterService).broadcast("s", "c")
    }
}
