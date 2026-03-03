package com.agent.backend

import com.agent.backend.db.entity.AgentUser
import com.agent.backend.db.entity.EmailVerificationToken
import com.agent.backend.db.rep.AgentUserRepository
import com.agent.backend.db.rep.EmailVerificationTokenRepository
import com.agent.backend.service.AuthService
import com.agent.backend.service.EmailService
import com.agent.backend.service.EmailVerificationService
import com.agent.backend.service.WalletService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional

class EmailVerificationServiceTest {

    private lateinit var tokenRepository: EmailVerificationTokenRepository
    private lateinit var userRepository: AgentUserRepository
    private lateinit var emailService: EmailService
    private lateinit var authService: AuthService
    private lateinit var walletService: WalletService
    private lateinit var service: EmailVerificationService

    private val userId = 1L
    private val userEmail = "test@example.com"

    @BeforeEach
    fun setup() {
        tokenRepository = mock(EmailVerificationTokenRepository::class.java)
        userRepository = mock(AgentUserRepository::class.java)
        emailService = mock(EmailService::class.java)
        authService = mock(AuthService::class.java)
        walletService = mock(WalletService::class.java)

        service = EmailVerificationService(
            emailVerificationTokenRepository = tokenRepository,
            agentUserRepository = userRepository,
            emailService = emailService,
            authService = authService,
            walletService = walletService,
            tokenTtlHours = 24,
            maxRequestsPerHour = 5,
            maxResendsPerToken = 3
        )
    }

    private fun makeUser() = AgentUser(
        id = userId,
        subject = "sub-$userId",
        email = userEmail,
        emailVerified = false
    )

    private fun makeToken(resendCount: Int = 0, lastResentAt: Instant? = null) = EmailVerificationToken(
        id = 1L,
        userId = userId,
        token = "test-token",
        email = userEmail,
        createdAt = Instant.now().minus(1, ChronoUnit.HOURS),
        expiresAt = Instant.now().plus(23, ChronoUnit.HOURS),
        resendCount = resendCount,
        lastResentAt = lastResentAt
    )

    // ── resend tracking behaviour ──────────────────────────────────────────────

    @Test
    fun `resendVerificationEmail should NOT update resend count when email send fails`() {
        val user = makeUser()
        val token = makeToken(lastResentAt = Instant.now().minus(10, ChronoUnit.MINUTES))

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(tokenRepository.findActiveByUserId(userId)).thenReturn(Optional.of(token))
        `when`(emailService.sendVerificationEmail(userEmail, "test-token")).thenReturn(false)

        val result = service.resendVerificationEmail(userId)

        assertFalse(result)
        assertEquals(0, token.resendCount, "resendCount must NOT be incremented on send failure")
        verify(tokenRepository, never()).save(token)
    }

    @Test
    fun `resendVerificationEmail should update resend count when email send succeeds`() {
        val user = makeUser()
        val previousLastResent = Instant.now().minus(10, ChronoUnit.MINUTES)
        val token = makeToken(lastResentAt = previousLastResent)

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(tokenRepository.findActiveByUserId(userId)).thenReturn(Optional.of(token))
        `when`(emailService.sendVerificationEmail(userEmail, "test-token")).thenReturn(true)
        `when`(tokenRepository.save(token)).thenReturn(token)

        val result = service.resendVerificationEmail(userId)

        assertTrue(result)
        assertEquals(1, token.resendCount)
        assertNotNull(token.lastResentAt)
        assertTrue(token.lastResentAt!!.isAfter(previousLastResent))
        verify(tokenRepository, times(1)).save(token)
    }

    @Test
    fun `resendVerificationEmail should preserve cooldown after consecutive failures`() {
        val user = makeUser()
        val token = makeToken(resendCount = 0, lastResentAt = Instant.now().minus(10, ChronoUnit.MINUTES))

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(tokenRepository.findActiveByUserId(userId)).thenReturn(Optional.of(token))
        `when`(emailService.sendVerificationEmail(userEmail, "test-token")).thenReturn(false)

        // First failed attempt
        service.resendVerificationEmail(userId)
        assertEquals(0, token.resendCount)

        // Second failed attempt — cooldown must still not be consumed
        service.resendVerificationEmail(userId)
        assertEquals(0, token.resendCount)
        verify(tokenRepository, never()).save(token)
    }

    // ── limit enforcement (unchanged behaviour) ────────────────────────────────

    @Test
    fun `resendVerificationEmail should throw when resend limit is reached`() {
        val user = makeUser()
        val token = makeToken(resendCount = 3) // equals maxResendsPerToken

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(tokenRepository.findActiveByUserId(userId)).thenReturn(Optional.of(token))

        assertThrows<IllegalStateException> {
            service.resendVerificationEmail(userId)
        }
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString())
    }

    @Test
    fun `resendVerificationEmail should throw when cooldown has not elapsed`() {
        val user = makeUser()
        val token = makeToken(lastResentAt = Instant.now().minus(2, ChronoUnit.MINUTES)) // < 5 min ago

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(tokenRepository.findActiveByUserId(userId)).thenReturn(Optional.of(token))

        assertThrows<IllegalStateException> {
            service.resendVerificationEmail(userId)
        }
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString())
    }

    @Test
    fun `resendVerificationEmail should throw when email is already verified`() {
        val user = makeUser().also { it.emailVerified = true }

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))

        assertThrows<IllegalStateException> {
            service.resendVerificationEmail(userId)
        }
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString())
    }

    @Test
    fun `resendVerificationEmail should create new token when no active token exists`() {
        val user = makeUser()

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(tokenRepository.findActiveByUserId(userId)).thenReturn(Optional.empty())
        `when`(tokenRepository.findAll()).thenReturn(emptyList())
        @Suppress("UNCHECKED_CAST")
        `when`(tokenRepository.save(org.mockito.ArgumentMatchers.any(EmailVerificationToken::class.java)))
            .thenAnswer { it.arguments[0] as EmailVerificationToken }
        `when`(userRepository.save(user)).thenReturn(user)
        `when`(emailService.sendVerificationEmail(anyString(), anyString())).thenReturn(true)

        val result = service.resendVerificationEmail(userId)

        assertTrue(result)
        verify(emailService, times(1)).sendVerificationEmail(anyString(), anyString())
    }
}
