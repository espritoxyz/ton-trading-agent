package com.agent.backend.email

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class EmailTemplateService {

    fun generateVerificationEmail(email: String, verificationLink: String, expiresInHours: Int, baseUrl: String): String {
        val template = loadTemplate("templates/email/verification-email.html")
        return template
            .replace("{{verificationLink}}", verificationLink)
            .replace("{{expiresInHours}}", expiresInHours.toString())
            .replace("{{baseUrl}}", baseUrl)
    }

    private fun loadTemplate(path: String): String {
        return ClassPathResource(path).inputStream.bufferedReader().use { it.readText() }
    }

    fun generatePasswordResetEmail(email: String, resetLink: String, expiresInHours: Int): String {
        // Placeholder for future implementation
        return ""
    }

    fun generateNewsletterEmail(subject: String, htmlContent: String, unsubscribeLink: String, baseUrl: String): String {
        val template = loadTemplate("templates/email/newsletter-email.html")
        return template
            .replace("{{subject}}", subject)
            .replace("{{content}}", htmlContent)
            .replace("{{unsubscribeLink}}", unsubscribeLink)
            .replace("{{baseUrl}}", baseUrl)
    }

    fun generateNewsletterVerificationEmail(confirmationLink: String, expiresInHours: Int, baseUrl: String): String {
        val template = loadTemplate("templates/email/newsletter-verification-email.html")
        return template
            .replace("{{confirmationLink}}", confirmationLink)
            .replace("{{expiresInHours}}", expiresInHours.toString())
            .replace("{{baseUrl}}", baseUrl)
    }
}
