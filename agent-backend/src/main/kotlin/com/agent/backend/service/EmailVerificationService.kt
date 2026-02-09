package com.agent.backend.service

import com.agent.backend.db.entity.AgentUser
import com.agent.backend.db.entity.EmailVerificationToken
import com.agent.backend.db.rep.AgentUserRepository
import com.agent.backend.db.rep.EmailVerificationTokenRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

enum class VerificationResult {
    SUCCESS,
    ALREADY_VERIFIED,
    EXPIRED,
    TOO_MANY_ATTEMPTS,
    INVALID_TOKEN
}

@Service
class EmailVerificationService(
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val agentUserRepository: AgentUserRepository,
    private val emailService: EmailService,
    private val authService: AuthService,
    @Value("\${email.verification.token-ttl-hours}") private val tokenTtlHours: Int,
    @Value("\${email.rate-limit.max-requests-per-hour}") private val maxRequestsPerHour: Int,
    @Value("\${email.rate-limit.max-resends-per-token}") private val maxResendsPerToken: Int
) {
    private val logger = KotlinLogging.logger {}

    private val secureRandom = SecureRandom()
    private val maxVerificationAttempts = 10
    private val minResendIntervalMinutes = 5L

    @Transactional
    fun createVerificationToken(user: AgentUser): EmailVerificationToken {
        // Check for existing active token
        val existingToken = emailVerificationTokenRepository.findActiveByUserId(user.id!!)
        if (existingToken.isPresent) {
            throw IllegalStateException("Active verification token already exists for this user")
        }

        // Check rate limit (max tokens per hour)
        checkRateLimit(user.id!!)

        // Generate secure token
        val token = generateSecureToken()

        // Create token entity
        val verificationToken = EmailVerificationToken(
            userId = user.id!!,
            token = token,
            email = user.email!!,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plus(tokenTtlHours.toLong(), ChronoUnit.HOURS)
        )

        val savedToken = emailVerificationTokenRepository.save(verificationToken)

        // Update user's emailVerificationSentAt
        user.emailVerificationSentAt = Instant.now()
        agentUserRepository.save(user)

        logger.info { "Created verification token for user ${user.id} (${user.email})" }

        return savedToken
    }

    fun sendVerificationEmail(user: AgentUser): Boolean {
        if (user.email == null) {
            throw IllegalArgumentException("User email is null")
        }

        if (user.emailVerified) {
            throw IllegalStateException("User email is already verified")
        }

        val token = createVerificationToken(user)
        return emailService.sendVerificationEmail(user.email!!, token.token)
    }

    @Transactional
    fun resendVerificationEmail(userId: Long): Boolean {
        val user = agentUserRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        if (user.emailVerified) {
            throw IllegalStateException("Email is already verified")
        }

        if (user.email == null) {
            throw IllegalArgumentException("User email is null")
        }

        // Find existing active token
        val existingTokenOpt = emailVerificationTokenRepository.findActiveByUserId(userId)

        if (existingTokenOpt.isPresent) {
            val existingToken = existingTokenOpt.get()

            // Check resend limits
            if (existingToken.resendCount >= maxResendsPerToken) {
                throw IllegalStateException("Maximum resend limit reached for this token. Please request a new verification email.")
            }

            // Check minimum interval between resends
            if (existingToken.lastResentAt != null) {
                val minutesSinceLastResend = ChronoUnit.MINUTES.between(existingToken.lastResentAt, Instant.now())
                if (minutesSinceLastResend < minResendIntervalMinutes) {
                    val waitMinutes = minResendIntervalMinutes - minutesSinceLastResend
                    throw IllegalStateException("Please wait $waitMinutes more minutes before requesting another email")
                }
            }

            // Update resend tracking
            existingToken.resendCount++
            existingToken.lastResentAt = Instant.now()
            emailVerificationTokenRepository.save(existingToken)

            logger.info { "Resending verification email for user $userId (resend count: ${existingToken.resendCount})" }

            return emailService.sendVerificationEmail(user.email!!, existingToken.token)
        } else {
            // No active token exists, create a new one
            logger.info { "No active token found for user $userId, creating new one" }
            return sendVerificationEmail(user)
        }
    }

    @Transactional
    fun verifyEmail(token: String): VerificationResult {
        val verificationTokenOpt = emailVerificationTokenRepository.findByToken(token)

        if (verificationTokenOpt.isEmpty) {
            logger.warn { "Verification attempted with invalid token" }
            return VerificationResult.INVALID_TOKEN
        }

        val verificationToken = verificationTokenOpt.get()

        // Check if already verified
        if (verificationToken.isVerified()) {
            logger.info { "Verification attempted for already verified token (user ${verificationToken.userId})" }
            return VerificationResult.ALREADY_VERIFIED
        }

        // Increment attempts counter
        verificationToken.attempts++
        emailVerificationTokenRepository.save(verificationToken)

        // Check attempts limit
        if (verificationToken.attempts > maxVerificationAttempts) {
            logger.warn { "Too many verification attempts for token (user ${verificationToken.userId})" }
            return VerificationResult.TOO_MANY_ATTEMPTS
        }

        // Check if expired
        if (verificationToken.isExpired()) {
            logger.info { "Verification attempted with expired token (user ${verificationToken.userId})" }
            return VerificationResult.EXPIRED
        }

        // Mark token as verified
        verificationToken.verifiedAt = Instant.now()
        emailVerificationTokenRepository.save(verificationToken)

        // Update user's emailVerified flag
        val user = agentUserRepository.findById(verificationToken.userId)
            .orElseThrow { IllegalStateException("User not found for verification token") }

        user.emailVerified = true
        agentUserRepository.save(user)

        // Update Keycloak emailVerified flag
        try {
            authService.updateKeycloakEmailVerified(user.subject, true)
        } catch (e: Exception) {
            logger.error(e) { "Failed to update Keycloak emailVerified for user ${user.id}" }
            // Don't fail the verification if Keycloak update fails
        }

        logger.info { "Email verified successfully for user ${user.id} (${user.email})" }

        return VerificationResult.SUCCESS
    }

    @Transactional
    fun cleanupExpiredTokens(): Int {
        val deletedCount = emailVerificationTokenRepository.deleteExpiredTokens(Instant.now())
        logger.info { "Cleaned up $deletedCount expired verification tokens" }
        return deletedCount
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(64)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun checkRateLimit(userId: Long) {
        // Simple rate limiting: count tokens created in the last hour
        // Note: This is a basic implementation. For production with multiple instances,
        // consider using Redis for distributed rate limiting
        val oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS)

        // We'll use a query to count recent tokens for this user
        // For simplicity, we'll fetch all tokens and filter (can be optimized with a custom query)
        val recentTokensCount = emailVerificationTokenRepository.findAll()
            .count { it.userId == userId && it.createdAt.isAfter(oneHourAgo) }

        if (recentTokensCount >= maxRequestsPerHour) {
            throw IllegalStateException("Rate limit exceeded. Maximum $maxRequestsPerHour verification emails per hour.")
        }
    }
}
