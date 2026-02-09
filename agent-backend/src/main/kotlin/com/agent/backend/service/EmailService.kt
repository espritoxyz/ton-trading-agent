package com.agent.backend.service

import com.agent.backend.email.EmailTemplateService
import com.agent.backend.email.ResendClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class EmailService(
    private val resendClient: ResendClient,
    private val emailTemplateService: EmailTemplateService,
    @Value("\${email.resend.from-email}") private val fromEmail: String,
    @Value("\${email.resend.from-name}") private val fromName: String,
    @Value("\${email.verification.base-url}") private val baseUrl: String,
    @Value("\${email.verification.token-ttl-hours}") private val tokenTtlHours: Int
) {
    private val logger = KotlinLogging.logger {}

    fun sendVerificationEmail(toEmail: String, token: String): Boolean {
        return try {
            val verificationLink = "$baseUrl/verify-email/$token"
            val from = "$fromName <$fromEmail>"

            val htmlContent = emailTemplateService.generateVerificationEmail(
                email = toEmail,
                verificationLink = verificationLink,
                expiresInHours = tokenTtlHours,
                baseUrl = baseUrl
            )

            logger.info { "Sending verification email to $toEmail" }

            resendClient.sendEmail(
                from = from,
                to = toEmail,
                subject = "Verify Your Email - Esprito AI",
                htmlBody = htmlContent
            )

            logger.info { "Verification email sent successfully to $toEmail" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to send verification email to $toEmail" }
            false
        }
    }

    fun sendPasswordResetEmail(toEmail: String, resetToken: String): Boolean {
        // Placeholder for future implementation
        logger.info { "Password reset email functionality not yet implemented" }
        return false
    }

    fun sendNewsletterEmail(toEmail: String, content: String): Boolean {
        // Placeholder for future implementation
        logger.info { "Newsletter email functionality not yet implemented" }
        return false
    }
}
