package com.agent.backend.scheduled

import com.agent.backend.service.EmailVerificationService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

@Component
class EmailVerificationCleanupTask(
    private val emailVerificationService: EmailVerificationService
) {

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun cleanupExpiredTokens() {
        logger.info { "Running scheduled cleanup of expired verification tokens" }
        try {
            val deleted = emailVerificationService.cleanupExpiredTokens()
            logger.info { "Cleanup completed: deleted $deleted expired tokens" }
        } catch (e: Exception) {
            logger.error(e) { "Error during cleanup of expired verification tokens" }
        }
    }
}
