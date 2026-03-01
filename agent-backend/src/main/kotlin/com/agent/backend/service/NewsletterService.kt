package com.agent.backend.service

import com.agent.backend.db.entity.ConfirmationIssuer
import com.agent.backend.db.entity.NewsletterStatus
import com.agent.backend.db.entity.NewsletterSubscription
import com.agent.backend.db.rep.NewsletterSubscriptionRepository
import com.agent.backend.dto.NewsletterBroadcastResponse
import com.agent.backend.email.EmailTemplateService
import com.agent.backend.email.ResendEmailRequest
import com.agent.backend.email.ResendClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID
private const val BATCH_SIZE = 100

@Service
class NewsletterService(
    private val repository: NewsletterSubscriptionRepository,
    private val resendClient: ResendClient,
    private val emailTemplateService: EmailTemplateService,
    private val broadcastJobStore: BroadcastJobStore,
    @Value("\${email.resend.from-email}") private val fromEmail: String,
    @Value("\${email.resend.from-name}") private val fromName: String,
    @Value("\${email.verification.base-url}") private val baseUrl: String,
    @Value("\${newsletter.verification-token-ttl-hours:48}") private val tokenTtlHours: Int,
    @Value("\${newsletter.resend-max-count:3}") private val resendMaxCount: Int,
    @Value("\${newsletter.resend-min-interval-minutes:5}") private val resendMinIntervalMinutes: Long
) {
    private val logger = KotlinLogging.logger {}
    private val secureRandom = SecureRandom()

    @Transactional
    fun subscribe(email: String): SubscribeResult {
        val existing = repository.findByEmail(email).orElse(null)

        if (existing != null) {
            return when (existing.status) {
                NewsletterStatus.ACTIVE -> SubscribeResult.ALREADY_SUBSCRIBED

                NewsletterStatus.PENDING_VERIFICATION -> {
                    val tokenExpired = existing.verificationTokenExpiresAt?.isBefore(Instant.now()) ?: true
                    if (!tokenExpired) {
                        // Token still valid — tell user to check inbox
                        SubscribeResult.PENDING_VERIFICATION
                    } else {
                        // Token expired — issue a new one and resend
                        issueVerificationToken(existing)
                        SubscribeResult.SUBSCRIBED
                    }
                }

                NewsletterStatus.UNSUBSCRIBED -> {
                    // UNSUBSCRIBED — re-subscribe flow
                    existing.status = NewsletterStatus.PENDING_VERIFICATION
                    existing.subscribedAt = Instant.now()
                    existing.unsubscribedAt = null
                    existing.confirmedAt = null
                    existing.confirmationIssuer = null
                    existing.resendCount = 0
                    existing.lastResentAt = null
                    issueVerificationToken(existing)
                    SubscribeResult.SUBSCRIBED
                }
            }
        }

        val subscription = NewsletterSubscription(
            email = email,
            unsubscribeToken = UUID.randomUUID().toString()
        )
        issueVerificationToken(subscription)
        return SubscribeResult.SUBSCRIBED
    }

    @Transactional
    fun confirmSubscription(token: String): ConfirmResult {
        val sub = repository.findByVerificationToken(token).orElse(null)
            ?: return ConfirmResult.INVALID_TOKEN

        if (sub.status == NewsletterStatus.ACTIVE) return ConfirmResult.ALREADY_CONFIRMED

        if (sub.verificationTokenExpiresAt?.isBefore(Instant.now()) == true) {
            return ConfirmResult.EXPIRED
        }

        sub.status = NewsletterStatus.ACTIVE
        sub.confirmedAt = Instant.now()
        sub.confirmationIssuer = ConfirmationIssuer.EMAIL_CONFIRMATION
        sub.verificationToken = null
        sub.verificationTokenExpiresAt = null
        repository.save(sub)
        logger.info { "Newsletter subscription confirmed for id=${sub.id}" }
        return ConfirmResult.CONFIRMED
    }

    @Transactional
    fun resendVerification(email: String): ResendResult {
        val sub = repository.findByEmail(email).orElse(null)
            ?: return ResendResult.NOT_FOUND

        if (sub.status == NewsletterStatus.ACTIVE) return ResendResult.ALREADY_CONFIRMED
        if (sub.status == NewsletterStatus.UNSUBSCRIBED) return ResendResult.NOT_FOUND

        if (sub.resendCount >= resendMaxCount) return ResendResult.MAX_RESENDS_REACHED

        sub.lastResentAt?.let { lastResent ->
            val minutesSince = ChronoUnit.MINUTES.between(lastResent, Instant.now())
            if (minutesSince < resendMinIntervalMinutes) return ResendResult.TOO_SOON
        }

        sub.resendCount++
        sub.lastResentAt = Instant.now()
        issueVerificationToken(sub)
        return ResendResult.SENT
    }

    @Transactional
    fun unsubscribeByEmail(email: String): UnsubscribeResult {
        val sub = repository.findByEmail(email).orElse(null)
            ?: return UnsubscribeResult.NOT_FOUND

        if (sub.status == NewsletterStatus.UNSUBSCRIBED) return UnsubscribeResult.ALREADY_UNSUBSCRIBED

        sub.status = NewsletterStatus.UNSUBSCRIBED
        sub.unsubscribedAt = Instant.now()
        repository.save(sub)
        logger.debug { "Unsubscribed id=${sub.id} from newsletter" }
        return UnsubscribeResult.UNSUBSCRIBED
    }

    @Transactional
    fun unsubscribeByToken(token: String): UnsubscribeResult {
        val sub = repository.findByUnsubscribeToken(token).orElse(null)
            ?: return UnsubscribeResult.NOT_FOUND

        if (sub.status == NewsletterStatus.UNSUBSCRIBED) return UnsubscribeResult.ALREADY_UNSUBSCRIBED

        sub.status = NewsletterStatus.UNSUBSCRIBED
        sub.unsubscribedAt = Instant.now()
        repository.save(sub)
        logger.debug { "Unsubscribed id=${sub.id} from newsletter via token" }
        return UnsubscribeResult.UNSUBSCRIBED
    }

    /**
     * Subscribes a registered user directly to ACTIVE status — no verification email sent.
     * [issuer] records the confirmation mechanism (see [ConfirmationIssuer]).
     */
    @Transactional
    fun subscribeRegisteredUser(email: String, issuer: ConfirmationIssuer) {
        val now = Instant.now()
        val existing = repository.findByEmail(email).orElse(null)
        if (existing != null) {
            if (existing.status == NewsletterStatus.ACTIVE) {
                // Already active — update issuer in case they re-confirmed via a different channel
                existing.confirmedAt = now
                existing.confirmationIssuer = issuer
                repository.save(existing)
                return
            }
            existing.status = NewsletterStatus.ACTIVE
            existing.confirmedAt = now
            existing.confirmationIssuer = issuer
            existing.subscribedAt = now
            existing.unsubscribedAt = null
            existing.verificationToken = null
            existing.verificationTokenExpiresAt = null
            repository.save(existing)
        } else {
            repository.save(
                NewsletterSubscription(
                    email = email,
                    unsubscribeToken = UUID.randomUUID().toString(),
                    status = NewsletterStatus.ACTIVE,
                    confirmedAt = now,
                    confirmationIssuer = issuer
                )
            )
        }
        logger.info { "Registered user subscribed to newsletter (issuer=$issuer)" }
    }

    /**
     * Returns true if the given email has an ACTIVE subscription.
     */
    fun getNewsletterStatus(email: String): Boolean {
        val sub = repository.findByEmail(email).orElse(null) ?: return false
        return sub.status == NewsletterStatus.ACTIVE
    }

    /**
     * Sets newsletter subscription for a registered user directly (no double opt-in).
     * [issuer] is recorded when subscribing; ignored on unsubscribe.
     */
    @Transactional
    fun setNewsletterSubscription(email: String, subscribed: Boolean, issuer: ConfirmationIssuer) {
        if (subscribed) {
            subscribeRegisteredUser(email, issuer)
        } else {
            unsubscribeByEmail(email)
        }
    }

    fun renderPreview(subject: String, htmlContent: String): String {
        return emailTemplateService.generateNewsletterEmail(
            subject = subject,
            htmlContent = htmlContent,
            unsubscribeLink = "#preview-unsubscribe",
            baseUrl = baseUrl
        )
    }

    /**
     * Sends the newsletter to all ACTIVE subscribers using paginated streaming to avoid
     * loading all records into memory. Runs asynchronously via {@code @Async}; the caller
     * receives a jobId immediately (202) and polls [BroadcastJobStore] for status.
     */
    @Async
    @Transactional(readOnly = true)
    fun broadcast(jobId: String, subject: String, htmlContent: String) {
        try {
            val total = repository.countByStatus(NewsletterStatus.ACTIVE)

            if (total == 0L) {
                logger.info { "Newsletter broadcast skipped: no active subscribers" }
                broadcastJobStore.complete(jobId, NewsletterBroadcastResponse(totalSubscribers = 0, sent = 0, failed = 0))
                return
            }

            logger.info { "Starting newsletter broadcast to $total subscribers: \"$subject\"" }

            val from = "$fromName <$fromEmail>"
            var sent = 0
            var failed = 0

            repository.streamAllByStatus(NewsletterStatus.ACTIVE).use { stream ->
                val iterator = stream.iterator()
                generateSequence { if (iterator.hasNext()) iterator.next() else null }
                    .chunked(BATCH_SIZE)
                    .forEachIndexed { chunkIndex, chunk ->
                        val emails = chunk.map { sub ->
                            val unsubscribeLink = "$baseUrl/newsletter/unsubscribe/${sub.unsubscribeToken}"
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
            }

            logger.info { "Newsletter broadcast complete: sent=$sent, failed=$failed, total=$total" }
            broadcastJobStore.complete(jobId, NewsletterBroadcastResponse(totalSubscribers = total, sent = sent, failed = failed))
        } catch (e: Exception) {
            logger.error(e) { "Newsletter broadcast job=$jobId failed with unexpected error" }
            broadcastJobStore.fail(jobId)
        }
    }

    /**
     * Generates a new verification token, persists it (inserting if new), and schedules the
     * confirmation email to be sent after the enclosing transaction commits. If called outside
     * a transaction (e.g. in tests), the email is sent immediately.
     */
    private fun issueVerificationToken(sub: NewsletterSubscription) {
        val token = generateSecureToken()
        sub.verificationToken = token
        sub.verificationTokenExpiresAt = Instant.now().plus(tokenTtlHours.toLong(), ChronoUnit.HOURS)
        repository.save(sub)

        val email = sub.email
        val confirmationLink = "$baseUrl/newsletter/confirm/$token"
        val html = emailTemplateService.generateNewsletterVerificationEmail(
            confirmationLink = confirmationLink,
            expiresInHours = tokenTtlHours,
            baseUrl = baseUrl
        )
        val from = "$fromName <$fromEmail>"

        val sendEmail = {
            try {
                resendClient.sendEmail(
                    from = from,
                    to = email,
                    subject = "Confirm your Esprito AI newsletter subscription",
                    htmlBody = html
                )
                logger.info { "Sent newsletter verification email" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to send newsletter verification email to $email" }
            }
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() = sendEmail()
            })
        } else {
            sendEmail()
        }
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(64)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}

enum class SubscribeResult { SUBSCRIBED, ALREADY_SUBSCRIBED, PENDING_VERIFICATION }
enum class ConfirmResult   { CONFIRMED, ALREADY_CONFIRMED, EXPIRED, INVALID_TOKEN }
enum class ResendResult    { SENT, TOO_SOON, MAX_RESENDS_REACHED, NOT_FOUND, ALREADY_CONFIRMED }
enum class UnsubscribeResult { UNSUBSCRIBED, ALREADY_UNSUBSCRIBED, NOT_FOUND }
