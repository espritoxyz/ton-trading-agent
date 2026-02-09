package com.agent.backend.email

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

private val logger = KotlinLogging.logger {}

class EmailSendException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

data class ResendEmailRequest(
    val from: String,
    val to: List<String>,
    val subject: String,
    val html: String
)

data class ResendEmailResponse(
    val id: String? = null,
    val message: String? = null
)

@Component
class ResendClient(
    @Value("\${email.resend.api-key}") private val apiKey: String,
    @Value("\${email.resend.api-url}") private val apiUrl: String
) {
    private val restClient = RestClient.create()

    fun sendEmail(from: String, to: String, subject: String, htmlBody: String): Boolean {
        return try {
            val request = ResendEmailRequest(
                from = from,
                to = listOf(to),
                subject = subject,
                html = htmlBody
            )

            logger.debug { "Sending email to $to via Resend API" }

            val response = restClient.post()
                .uri("$apiUrl/emails")
                .header("Authorization", "Bearer $apiKey")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body<ResendEmailResponse>()

            logger.info { "Email sent successfully to $to. Response ID: ${response?.id}" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to send email to $to" }
            throw EmailSendException("Failed to send email to $to: ${e.message}", e)
        }
    }
}
