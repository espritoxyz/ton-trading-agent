package com.agent.backend.service

import com.agent.backend.db.entity.NewsletterSubscription
import com.agent.backend.db.rep.NewsletterSubscriptionRepository
import com.agent.backend.dto.NewsletterBroadcastResponse
import com.agent.backend.email.EmailTemplateService
import com.agent.backend.email.ResendEmailRequest
import com.agent.backend.email.ResendClient
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

private const val BATCH_SIZE = 100

@Service
class NewsletterService(
    private val repository: NewsletterSubscriptionRepository,
    private val resendClient: ResendClient,
    private val emailTemplateService: EmailTemplateService,
    @Value("\${email.resend.from-email}") private val fromEmail: String,
    @Value("\${email.resend.from-name}") private val fromName: String,
    @Value("\${email.verification.base-url}") private val baseUrl: String
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    fun subscribe(email: String): SubscribeResult {
        val existing = repository.findByEmail(email)

        if (existing.isPresent) {
            val sub = existing.get()
            return if (sub.active) {
                SubscribeResult.ALREADY_SUBSCRIBED
            } else {
                sub.active = true
                sub.subscribedAt = Instant.now()
                sub.unsubscribedAt = null
                sub.unsubscribeToken = UUID.randomUUID().toString()
                repository.save(sub)
                logger.info { "Re-subscribed $email to newsletter" }
                SubscribeResult.SUBSCRIBED
            }
        }

        val subscription = NewsletterSubscription(
            email = email,
            unsubscribeToken = UUID.randomUUID().toString()
        )
        repository.save(subscription)
        logger.info { "New newsletter subscription: $email" }
        return SubscribeResult.SUBSCRIBED
    }

    @Transactional
    fun unsubscribeByEmail(email: String): UnsubscribeResult {
        val sub = repository.findByEmail(email).orElse(null)
            ?: return UnsubscribeResult.NOT_FOUND

        if (!sub.active) return UnsubscribeResult.ALREADY_UNSUBSCRIBED

        sub.active = false
        sub.unsubscribedAt = Instant.now()
        repository.save(sub)
        logger.info { "Unsubscribed $email from newsletter" }
        return UnsubscribeResult.UNSUBSCRIBED
    }

    @Transactional
    fun unsubscribeByToken(token: String): UnsubscribeResult {
        val sub = repository.findByUnsubscribeToken(token).orElse(null)
            ?: return UnsubscribeResult.NOT_FOUND

        if (!sub.active) return UnsubscribeResult.ALREADY_UNSUBSCRIBED

        sub.active = false
        sub.unsubscribedAt = Instant.now()
        repository.save(sub)
        logger.info { "Unsubscribed ${sub.email} from newsletter via token" }
        return UnsubscribeResult.UNSUBSCRIBED
    }

    fun renderPreview(subject: String, htmlContent: String): String {
        return emailTemplateService.generateNewsletterEmail(
            subject = subject,
            htmlContent = htmlContent,
            unsubscribeLink = "#preview-unsubscribe",
            baseUrl = baseUrl
        )
    }

    fun broadcast(subject: String, htmlContent: String): NewsletterBroadcastResponse {
        val subscribers = repository.findAllByActive(true)
        val total = subscribers.size

        if (subscribers.isEmpty()) {
            logger.info { "Newsletter broadcast skipped: no active subscribers" }
            return NewsletterBroadcastResponse(totalSubscribers = 0, sent = 0, failed = 0)
        }

        logger.info { "Starting newsletter broadcast to $total subscribers: \"$subject\"" }

        val from = "$fromName <$fromEmail>"
        var sent = 0
        var failed = 0

        subscribers
            .chunked(BATCH_SIZE)
            .forEachIndexed { chunkIndex, chunk ->
                val emails = chunk.map { sub ->
                    val unsubscribeLink = "$baseUrl/api/newsletter/unsubscribe/${sub.unsubscribeToken}"
                    val html = emailTemplateService.generateNewsletterEmail(
                        subject = subject,
                        htmlContent = htmlContent,
                        unsubscribeLink = unsubscribeLink,
                        baseUrl = baseUrl
                    )
                    ResendEmailRequest(from = from, to = listOf(sub.email), subject = subject, html = html)
                }

                try {
                    val accepted = resendClient.sendBatch(emails)
                    sent += accepted
                    val skipped = chunk.size - accepted
                    if (skipped > 0) {
                        logger.warn { "Batch $chunkIndex: $accepted accepted, $skipped not accepted by Resend" }
                        failed += skipped
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Batch $chunkIndex failed (${chunk.size} emails)" }
                    failed += chunk.size
                }
            }

        logger.info { "Newsletter broadcast complete: sent=$sent, failed=$failed, total=$total" }
        return NewsletterBroadcastResponse(totalSubscribers = total, sent = sent, failed = failed)
    }
}

enum class SubscribeResult { SUBSCRIBED, ALREADY_SUBSCRIBED }
enum class UnsubscribeResult { UNSUBSCRIBED, ALREADY_UNSUBSCRIBED, NOT_FOUND }
