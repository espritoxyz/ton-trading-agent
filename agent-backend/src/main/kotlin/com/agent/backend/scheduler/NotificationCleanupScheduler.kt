package com.agent.backend.scheduler

import com.agent.backend.db.rep.NotificationRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class NotificationCleanupScheduler(
    private val notificationRepository: NotificationRepository,
    @Value("\${notification.retention.days:90}")
    private val retentionDays: Long
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    /**
     * Runs daily at midnight to delete notifications older than the retention period.
     * Cron expression: second minute hour day month weekday
     * 0 0 0 * * * = every day at midnight
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    fun cleanupOldNotifications() {
        try {
            val cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS)

            logger.info { "Starting notification cleanup job (retention: $retentionDays days, cutoff: $cutoffDate)" }

            // Delete old notifications efficiently using a query
            val deletedCount = notificationRepository.deleteByCreatedAtBefore(cutoffDate)

            logger.info { "Notification cleanup job completed. Deleted $deletedCount notifications older than $retentionDays days" }

        } catch (e: Exception) {
            logger.error(e) { "Notification cleanup job failed" }
        }
    }
}
