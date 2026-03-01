package com.agent.backend

import com.agent.backend.db.entity.ConfirmationIssuer
import com.agent.backend.db.entity.NewsletterStatus
import com.agent.backend.db.entity.NewsletterSubscription
import com.agent.backend.db.rep.NewsletterSubscriptionRepository
import com.agent.backend.dto.NewsletterBroadcastResponse
import com.agent.backend.email.EmailTemplateService
import com.agent.backend.email.ResendClient
import com.agent.backend.service.ConfirmResult
import com.agent.backend.service.NewsletterService
import com.agent.backend.service.ResendResult
import com.agent.backend.service.SubscribeResult
import com.agent.backend.service.UnsubscribeResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

// Helper functions to work around Mockito/Kotlin nullability issues
private fun anyString(): String = ArgumentMatchers.anyString() ?: ""
private fun <T> any(type: Class<T>): T = ArgumentMatchers.any(type)

class NewsletterServiceTest {

    private lateinit var repository: NewsletterSubscriptionRepository
    private lateinit var resendClient: ResendClient
    private lateinit var emailTemplateService: EmailTemplateService

    private lateinit var service: NewsletterService

    private val fromEmail = "no-reply@example.com"
    private val fromName = "Example"
    private val baseUrl = "https://example.com"

    @BeforeEach
    fun setup() {
        repository = mock(NewsletterSubscriptionRepository::class.java)
        resendClient = mock(ResendClient::class.java)
        emailTemplateService = mock(EmailTemplateService::class.java)

        service = NewsletterService(
            repository = repository,
            resendClient = resendClient,
            emailTemplateService = emailTemplateService,
            fromEmail = fromEmail,
            fromName = fromName,
            baseUrl = baseUrl,
            tokenTtlHours = 48,
            resendMaxCount = 3,
            resendMinIntervalMinutes = 5
        )

        `when`(
            emailTemplateService.generateNewsletterVerificationEmail(
                anyString(),
                ArgumentMatchers.anyInt(),
                anyString()
            )
        ).thenReturn("<html>verification</html>")

        `when`(
            emailTemplateService.generateNewsletterEmail(
                anyString(),
                anyString(),
                anyString(),
                anyString()
            )
        ).thenReturn("<html>newsletter</html>")

        `when`(resendClient.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(true)
    }

    @Test
    fun `subscribe should create new pending subscription and send verification`() {
        // Given
        val email = "user@example.com"
        `when`(repository.findByEmail(email)).thenReturn(Optional.empty())

        val captor = ArgumentCaptor.forClass(NewsletterSubscription::class.java)
        `when`(repository.save(captor.capture())).thenAnswer { it.arguments[0] as NewsletterSubscription }

        // When
        val result = service.subscribe(email)

        // Then
        assertEquals(SubscribeResult.SUBSCRIBED, result)

        val saved = captor.value
        assertEquals(email, saved.email)
        assertEquals(NewsletterStatus.PENDING_VERIFICATION, saved.status)
        assertNotNull(saved.unsubscribeToken)
        assertNotNull(saved.verificationToken)
        assertNotNull(saved.verificationTokenExpiresAt)
        assertTrue(saved.verificationTokenExpiresAt!!.isAfter(Instant.now()))

        verify(resendClient, times(1)).sendEmail(
            from = "$fromName <$fromEmail>",
            to = email,
            subject = "Confirm your Esprito AI newsletter subscription",
            htmlBody = "<html>verification</html>"
        )
    }

    @Test
    fun `subscribe should return already subscribed when status active`() {
        // Given
        val email = "active@example.com"
        val sub = NewsletterSubscription(
            id = 1L,
            email = email,
            unsubscribeToken = UUID.randomUUID().toString(),
            status = NewsletterStatus.ACTIVE
        )
        `when`(repository.findByEmail(email)).thenReturn(Optional.of(sub))

        // When
        val result = service.subscribe(email)

        // Then
        assertEquals(SubscribeResult.ALREADY_SUBSCRIBED, result)
        verify(repository, never()).save(any(NewsletterSubscription::class.java))
        verify(resendClient, never()).sendEmail(anyString(), anyString(), anyString(), anyString())
    }

    @Test
    fun `subscribe pending with valid token should return pending verification without resending`() {
        // Given
        val email = "pending@example.com"
        val sub = NewsletterSubscription(
            id = 1L,
            email = email,
            unsubscribeToken = UUID.randomUUID().toString(),
            status = NewsletterStatus.PENDING_VERIFICATION,
            verificationToken = "token",
            verificationTokenExpiresAt = Instant.now().plus(1, ChronoUnit.HOURS)
        )
        `when`(repository.findByEmail(email)).thenReturn(Optional.of(sub))

        // When
        val result = service.subscribe(email)

        // Then
        assertEquals(SubscribeResult.PENDING_VERIFICATION, result)
        verify(repository, never()).save(any(NewsletterSubscription::class.java))
        verify(resendClient, never()).sendEmail(anyString(), anyString(), anyString(), anyString())
    }

    @Test
    fun `subscribe pending with expired token should issue new token and resend`() {
        // Given
        val email = "pending-expired@example.com"
        val sub = NewsletterSubscription(
            id = 1L,
            email = email,
            unsubscribeToken = UUID.randomUUID().toString(),
            status = NewsletterStatus.PENDING_VERIFICATION,
            verificationToken = "old-token",
            verificationTokenExpiresAt = Instant.now().minus(1, ChronoUnit.HOURS)
        )
        `when`(repository.findByEmail(email)).thenReturn(Optional.of(sub))
        `when`(repository.save(any(NewsletterSubscription::class.java))).thenAnswer { it.arguments[0] as NewsletterSubscription }

        // When
        val result = service.subscribe(email)

        // Then
        assertEquals(SubscribeResult.SUBSCRIBED, result)
        assertNotNull(sub.verificationToken)
        assertTrue(sub.verificationToken!! != "old-token")
        verify(resendClient, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString())
    }

    @Test
    fun `subscribe unsubscribed should reset and issue new token`() {
        // Given
        val email = "unsub@example.com"
        val sub = NewsletterSubscription(
            id = 1L,
            email = email,
            unsubscribeToken = UUID.randomUUID().toString(),
            status = NewsletterStatus.UNSUBSCRIBED,
            unsubscribedAt = Instant.now().minus(3, ChronoUnit.DAYS),
            confirmedAt = Instant.now().minus(10, ChronoUnit.DAYS),
            confirmationIssuer = ConfirmationIssuer.EMAIL_CONFIRMATION,
            resendCount = 2,
            lastResentAt = Instant.now().minus(1, ChronoUnit.DAYS)
        )
        `when`(repository.findByEmail(email)).thenReturn(Optional.of(sub))
        `when`(repository.save(any(NewsletterSubscription::class.java))).thenAnswer { it.arguments[0] as NewsletterSubscription }

        // When
        val result = service.subscribe(email)

        // Then
        assertEquals(SubscribeResult.SUBSCRIBED, result)
        assertEquals(NewsletterStatus.PENDING_VERIFICATION, sub.status)
        assertNull(sub.unsubscribedAt)
        assertNull(sub.confirmedAt)
        assertNull(sub.confirmationIssuer)
        assertEquals(0, sub.resendCount)
        assertNull(sub.lastResentAt)
        assertNotNull(sub.verificationToken)
        verify(resendClient, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString())
    }

    @Test
    fun `confirmSubscription should return invalid token when not found`() {
        // Given
        `when`(repository.findByVerificationToken("bad")).thenReturn(Optional.empty())

        // When
        val result = service.confirmSubscription("bad")

        // Then
        assertEquals(ConfirmResult.INVALID_TOKEN, result)
    }

    @Test
    fun `confirmSubscription should return expired when token expired`() {
        // Given
        val sub = NewsletterSubscription(
            id = 1L,
            email = "x@example.com",
            unsubscribeToken = UUID.randomUUID().toString(),
            status = NewsletterStatus.PENDING_VERIFICATION,
            verificationToken = "token",
            verificationTokenExpiresAt = Instant.now().minus(1, ChronoUnit.MINUTES)
        )
        `when`(repository.findByVerificationToken("token")).thenReturn(Optional.of(sub))

        // When
        val result = service.confirmSubscription("token")

        // Then
        assertEquals(ConfirmResult.EXPIRED, result)
        assertEquals(NewsletterStatus.PENDING_VERIFICATION, sub.status)
        verify(repository, never()).save(any(NewsletterSubscription::class.java))
    }

    @Test
    fun `confirmSubscription should return already confirmed for active sub`() {
        // Given
        val sub = NewsletterSubscription(
            id = 1L,
            email = "x@example.com",
            unsubscribeToken = UUID.randomUUID().toString(),
            status = NewsletterStatus.ACTIVE
        )
        `when`(repository.findByVerificationToken("token")).thenReturn(Optional.of(sub))

        // When
        val result = service.confirmSubscription("token")

        // Then
        assertEquals(ConfirmResult.ALREADY_CONFIRMED, result)
        verify(repository, never()).save(any(NewsletterSubscription::class.java))
    }

    @Test
    fun `confirmSubscription should activate subscription and clear token`() {
        // Given
        val sub = NewsletterSubscription(
            id = 1L,
            email = "x@example.com",
            unsubscribeToken = UUID.randomUUID().toString(),
            status = NewsletterStatus.PENDING_VERIFICATION,
            verificationToken = "token",
            verificationTokenExpiresAt = Instant.now().plus(10, ChronoUnit.MINUTES)
        )
        `when`(repository.findByVerificationToken("token")).thenReturn(Optional.of(sub))
        `when`(repository.save(any(NewsletterSubscription::class.java))).thenAnswer { it.arguments[0] as NewsletterSubscription }

        // When
        val result = service.confirmSubscription("token")

        // Then
        assertEquals(ConfirmResult.CONFIRMED, result)
        assertEquals(NewsletterStatus.ACTIVE, sub.status)
        assertNotNull(sub.confirmedAt)
        assertEquals(ConfirmationIssuer.EMAIL_CONFIRMATION, sub.confirmationIssuer)
        assertNull(sub.verificationToken)
        assertNull(sub.verificationTokenExpiresAt)
        verify(repository, times(1)).save(sub)
    }

    @Test
    fun `resendVerification should be too soon if interval not passed`() {
        // Given
        val email = "pending@example.com"
        val sub = NewsletterSubscription(
            id = 1L,
            email = email,
            unsubscribeToken = UUID.randomUUID().toString(),
            status = NewsletterStatus.PENDING_VERIFICATION,
            resendCount = 0,
            lastResentAt = Instant.now().minus(1, ChronoUnit.MINUTES)
        )
        `when`(repository.findByEmail(email)).thenReturn(Optional.of(sub))

        // When
        val result = service.resendVerification(email)

        // Then
        assertEquals(ResendResult.TOO_SOON, result)
        verify(repository, never()).save(any(NewsletterSubscription::class.java))
    }

    @Test
    fun `resendVerification should return max reached`() {
        // Given
        val email = "pending@example.com"
        val sub = NewsletterSubscription(
            id = 1L,
            email = email,
            unsubscribeToken = UUID.randomUUID().toString(),
            status = NewsletterStatus.PENDING_VERIFICATION,
            resendCount = 3
        )
        `when`(repository.findByEmail(email)).thenReturn(Optional.of(sub))

        // When
        val result = service.resendVerification(email)

        // Then
        assertEquals(ResendResult.MAX_RESENDS_REACHED, result)
        verify(repository, never()).save(any(NewsletterSubscription::class.java))
    }

    @Test
    fun `resendVerification should send and increment resend counters`() {
        // Given
        val email = "pending@example.com"
        val sub = NewsletterSubscription(
            id = 1L,
            email = email,
            unsubscribeToken = UUID.randomUUID().toString(),
            status = NewsletterStatus.PENDING_VERIFICATION,
            resendCount = 0,
            lastResentAt = Instant.now().minus(10, ChronoUnit.MINUTES)
        )
        `when`(repository.findByEmail(email)).thenReturn(Optional.of(sub))
        `when`(repository.save(any(NewsletterSubscription::class.java))).thenAnswer { it.arguments[0] as NewsletterSubscription }

        // When
        val result = service.resendVerification(email)

        // Then
        assertEquals(ResendResult.SENT, result)
        assertEquals(1, sub.resendCount)
        assertNotNull(sub.lastResentAt)
        assertNotNull(sub.verificationToken)
        verify(resendClient, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString())
    }

    @Test
    fun `resendVerification should return not found for unsubscribed user`() {
        // Given
        val email = "unsub@example.com"
        val sub = NewsletterSubscription(
            id = 1L,
            email = email,
            unsubscribeToken = UUID.randomUUID().toString(),
            status = NewsletterStatus.UNSUBSCRIBED,
            unsubscribedAt = Instant.now().minus(1, ChronoUnit.DAYS)
        )
        `when`(repository.findByEmail(email)).thenReturn(Optional.of(sub))

        // When
        val result = service.resendVerification(email)

        // Then: NOT_FOUND so the caller returns a vague response (no token re-issued, no email sent)
        assertEquals(ResendResult.NOT_FOUND, result)
        verify(repository, never()).save(any(NewsletterSubscription::class.java))
        verify(resendClient, never()).sendEmail(anyString(), anyString(), anyString(), anyString())
    }

    @Test
    fun `unsubscribeByEmail should unsubscribe active subscription`() {
        // Given
        val email = "a@example.com"
        val sub = NewsletterSubscription(
            id = 1L,
            email = email,
            unsubscribeToken = UUID.randomUUID().toString(),
            status = NewsletterStatus.ACTIVE
        )
        `when`(repository.findByEmail(email)).thenReturn(Optional.of(sub))
        `when`(repository.save(any(NewsletterSubscription::class.java))).thenAnswer { it.arguments[0] as NewsletterSubscription }

        // When
        val result = service.unsubscribeByEmail(email)

        // Then
        assertEquals(UnsubscribeResult.UNSUBSCRIBED, result)
        assertEquals(NewsletterStatus.UNSUBSCRIBED, sub.status)
        assertNotNull(sub.unsubscribedAt)
        verify(repository, times(1)).save(sub)
    }

    @Test
    fun `getNewsletterStatus should return true only for active`() {
        // Given
        val email = "x@example.com"
        `when`(repository.findByEmail(email)).thenReturn(
            Optional.of(
                NewsletterSubscription(
                    id = 1L,
                    email = email,
                    unsubscribeToken = UUID.randomUUID().toString(),
                    status = NewsletterStatus.ACTIVE
                )
            )
        )

        // When/Then
        assertTrue(service.getNewsletterStatus(email))

        // Given not active
        `when`(repository.findByEmail(email)).thenReturn(
            Optional.of(
                NewsletterSubscription(
                    id = 1L,
                    email = email,
                    unsubscribeToken = UUID.randomUUID().toString(),
                    status = NewsletterStatus.UNSUBSCRIBED
                )
            )
        )
        assertFalse(service.getNewsletterStatus(email))

        // Given not found
        `when`(repository.findByEmail(email)).thenReturn(Optional.empty())
        assertFalse(service.getNewsletterStatus(email))
    }

    @Test
    fun `broadcast should return zeros when no active subscribers`() {
        // Given
        `when`(repository.countByStatus(NewsletterStatus.ACTIVE)).thenReturn(0L)

        // When — @Async is not applied in unit tests (no Spring proxy), so get() resolves immediately
        val result = service.broadcast("Sub", "<p>x</p>").get()

        // Then
        assertEquals(NewsletterBroadcastResponse(0L, 0, 0), result)
        verify(resendClient, never()).sendBatch(ArgumentMatchers.anyList())
    }

    @Test
    fun `broadcast should send batch and count accepted and failed`() {
        // Given
        val subs = (1..3).map {
            NewsletterSubscription(
                id = it.toLong(),
                email = "u$it@example.com",
                unsubscribeToken = "tok-$it",
                status = NewsletterStatus.ACTIVE
            )
        }
        `when`(repository.countByStatus(NewsletterStatus.ACTIVE)).thenReturn(3L)
        `when`(repository.streamAllByStatus(NewsletterStatus.ACTIVE)).thenReturn(subs.stream())
        `when`(resendClient.sendBatch(ArgumentMatchers.anyList())).thenReturn(2)

        // When
        val result = service.broadcast("Sub", "<p>x</p>").get()

        // Then
        assertEquals(3L, result.totalSubscribers)
        assertEquals(2, result.sent)
        assertEquals(1, result.failed)
        verify(resendClient, times(1)).sendBatch(ArgumentMatchers.anyList())
    }

    @Test
    fun `broadcast should process multiple batches`() {
        // Given — 250 subscribers triggers 3 batches (100 + 100 + 50)
        val subs = (1..250).map {
            NewsletterSubscription(
                id = it.toLong(),
                email = "u$it@example.com",
                unsubscribeToken = "tok-$it",
                status = NewsletterStatus.ACTIVE
            )
        }
        `when`(repository.countByStatus(NewsletterStatus.ACTIVE)).thenReturn(250L)
        `when`(repository.streamAllByStatus(NewsletterStatus.ACTIVE)).thenReturn(subs.stream())
        `when`(resendClient.sendBatch(ArgumentMatchers.anyList())).thenReturn(100).thenReturn(100).thenReturn(50)

        // When
        val result = service.broadcast("Sub", "<p>x</p>").get()

        // Then
        assertEquals(250L, result.totalSubscribers)
        assertEquals(250, result.sent)
        assertEquals(0, result.failed)
        verify(resendClient, times(3)).sendBatch(ArgumentMatchers.anyList())
    }
}
