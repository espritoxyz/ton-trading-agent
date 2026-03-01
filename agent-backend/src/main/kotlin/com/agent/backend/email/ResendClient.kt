package com.agent.backend.email

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
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
    @Value("\${email.resend.api-url}") private val apiUrl: String,
    @Value("\${email.resend.retry.max-attempts:3}") private val maxRetryAttempts: Int,
    @Value("\${email.resend.retry.delay-ms:500}") private val retryDelayMs: Long,
    @Qualifier("resendRestClient") private val restClient: RestClient
) {
    fun sendEmail(from: String, to: String, subject: String, htmlBody: String): Boolean {
        val request = ResendEmailRequest(
            from = from,
            to = listOf(to),
            subject = subject,
            html = htmlBody
        )

        var lastException: Exception? = null

        repeat(maxRetryAttempts) { attempt ->
            try {
                logger.debug { "Sending email to $to via Resend API (attempt ${attempt + 1}/$maxRetryAttempts)" }

                val response = restClient.post()
                    .uri("$apiUrl/emails")
                    .header("Authorization", "Bearer $apiKey")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body<ResendEmailResponse>()

                logger.info { "Email sent successfully to $to. Response ID: ${response?.id}" }
                return true
            } catch (e: HttpClientErrorException) {
                // 4xx errors are permanent (invalid API key, bad request, etc.) — do not retry
                logger.error(e) { "HTTP ${e.statusCode} client error sending email to $to: ${e.responseBodyAsString}" }
                throw EmailSendException("HTTP ${e.statusCode} client error sending email to $to", e)
            } catch (e: HttpServerErrorException) {
                // 5xx errors are transient — retry
                logger.warn { "HTTP ${e.statusCode} server error sending email to $to (attempt ${attempt + 1}/$maxRetryAttempts): ${e.responseBodyAsString}" }
                lastException = e
            } catch (e: ResourceAccessException) {
                // Network/timeout errors — retry
                logger.warn { "Network/timeout error sending email to $to (attempt ${attempt + 1}/$maxRetryAttempts): ${e.message}" }
                lastException = e
            }

            if (attempt < maxRetryAttempts - 1) {
                Thread.sleep(retryDelayMs)
            }
        }

        logger.error(lastException) { "Failed to send email to $to after $maxRetryAttempts attempts" }
        throw EmailSendException("Failed to send email to $to after $maxRetryAttempts attempts", lastException)
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
        } catch (e: HttpClientErrorException) {
            logger.error(e) { "HTTP ${e.statusCode} client error in batch send for ${emails.size} emails: ${e.responseBodyAsString}" }
            throw EmailSendException("Batch send failed with HTTP ${e.statusCode} client error", e)
        } catch (e: HttpServerErrorException) {
            logger.error(e) { "HTTP ${e.statusCode} server error in batch send for ${emails.size} emails" }
            throw EmailSendException("Batch send failed with HTTP ${e.statusCode} server error", e)
        } catch (e: ResourceAccessException) {
            logger.error(e) { "Network/timeout error in batch send for ${emails.size} emails: ${e.message}" }
            throw EmailSendException("Batch send failed due to network/timeout error: ${e.message}", e)
        }
    }
}
