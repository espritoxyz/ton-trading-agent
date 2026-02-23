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

data class ResendBatchResponse(
    val data: List<ResendEmailResponse>? = null
)

@Component
class ResendClient(
    @Value("\${email.resend.api-key}") private val apiKey: String,
    @Value("\${email.resend.api-url}") private val apiUrl: String
) {
    private val restClient = RestClient.builder()
        .requestFactory(org.springframework.http.client.SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(java.time.Duration.ofSeconds(5))
            setReadTimeout(java.time.Duration.ofSeconds(10))
        })
        .build()

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

    /**
     * Sends up to 100 emails in a single API request using Resend Batch API.
     * Returns the number of emails accepted by Resend.
     */
    fun sendBatch(emails: List<ResendEmailRequest>): Int {
        require(emails.isNotEmpty()) { "Batch must not be empty" }
        require(emails.size <= 100) { "Batch size must not exceed 100" }

        return try {
            logger.debug { "Sending batch of ${emails.size} emails via Resend Batch API" }

            val response = restClient.post()
                .uri("$apiUrl/emails/batch")
                .header("Authorization", "Bearer $apiKey")
                .contentType(MediaType.APPLICATION_JSON)
                .body(emails)
                .retrieve()
                .body<ResendBatchResponse>()

            val accepted = response?.data?.size ?: 0
            logger.info { "Batch accepted by Resend: $accepted / ${emails.size}" }
            accepted
        } catch (e: Exception) {
            logger.error(e) { "Batch send failed for ${emails.size} emails" }
            throw EmailSendException("Batch send failed: ${e.message}", e)
        }
    }
}
