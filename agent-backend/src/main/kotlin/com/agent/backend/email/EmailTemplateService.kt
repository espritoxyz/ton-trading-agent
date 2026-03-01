package com.agent.backend.email

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class EmailTemplateService {

    fun generateVerificationEmail(email: String, verificationLink: String, expiresInHours: Int, baseUrl: String): String {
        val template = loadTemplate("templates/email/verification-email.html")
        return applyTemplate(template, mapOf(
            "{{verificationLink}}" to verificationLink,
            "{{expiresInHours}}" to expiresInHours.toString(),
            "{{baseUrl}}" to baseUrl
        ))
    }

    fun generatePasswordResetEmail(email: String, resetLink: String, expiresInHours: Int): String {
        // Placeholder for future implementation
        return ""
    }

    fun generateNewsletterEmail(subject: String, htmlContent: String, unsubscribeLink: String, baseUrl: String): String {
        val template = loadTemplate("templates/email/newsletter-email.html")
        return applyTemplate(template, mapOf(
            "{{subject}}" to subject,
            "{{content}}" to htmlContent,
            "{{unsubscribeLink}}" to unsubscribeLink,
            "{{baseUrl}}" to baseUrl
        ))
    }

    fun generateNewsletterVerificationEmail(confirmationLink: String, expiresInHours: Int, baseUrl: String): String {
        val template = loadTemplate("templates/email/newsletter-verification-email.html")
        return applyTemplate(template, mapOf(
            "{{confirmationLink}}" to confirmationLink,
            "{{expiresInHours}}" to expiresInHours.toString(),
            "{{baseUrl}}" to baseUrl
        ))
    }

    private fun loadTemplate(path: String): String {
        return ClassPathResource(path).inputStream.bufferedReader().use { it.readText() }
    }

    /**
     * Replaces all template placeholders in a single regex pass to prevent injection
     * where one substituted value could itself contain another placeholder key.
     */
    private fun applyTemplate(template: String, replacements: Map<String, String>): String {
        val pattern = Regex(replacements.keys.joinToString("|") { Regex.escape(it) })
        return pattern.replace(template) { match -> replacements[match.value] ?: match.value }
    }
}
