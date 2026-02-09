package com.agent.backend.email

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class EmailTemplateService {

    fun generateVerificationEmail(email: String, verificationLink: String, expiresInHours: Int): String {
        val template = loadTemplate("templates/email/verification-email.html")
        return template
            .replace("{{verificationLink}}", verificationLink)
            .replace("{{expiresInHours}}", expiresInHours.toString())
    }

    private fun loadTemplate(path: String): String {
        return ClassPathResource(path).inputStream.bufferedReader().use { it.readText() }
    }

    fun generatePasswordResetEmail(email: String, resetLink: String, expiresInHours: Int): String {
        // Placeholder for future implementation
        return ""
    }

    fun generateNewsletterEmail(content: String): String {
        // Placeholder for future implementation
        return ""
    }
}
