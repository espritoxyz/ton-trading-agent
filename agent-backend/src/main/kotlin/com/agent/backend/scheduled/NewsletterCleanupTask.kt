package com.agent.backend.scheduled

import com.agent.backend.db.entity.NewsletterStatus
import com.agent.backend.db.rep.NewsletterSubscriptionRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class NewsletterCleanupTask(
    private val repository: NewsletterSubscriptionRepository,
    @Value("\${newsletter.pending-verification-ttl-days:7}") private val pendingTtlDays: Long
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    /** Runs daily at 2 AM to remove stale PENDING_VERIFICATION subscriptions. */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    fun cleanupStalePendingSubscriptions() {
        try {
            val cutoff = Instant.now().minus(pendingTtlDays, ChronoUnit.DAYS)
            logger.info { "Starting newsletter cleanup (pending TTL: $pendingTtlDays days, cutoff: $cutoff)" }
            val deleted = repository.deleteStalePending(NewsletterStatus.PENDING_VERIFICATION, cutoff)
            logger.info { "Deleted $deleted stale PENDING_VERIFICATION newsletter subscriptions" }
        } catch (e: Exception) {
            logger.error(e) { "Newsletter cleanup task failed" }
        }
    }
}
