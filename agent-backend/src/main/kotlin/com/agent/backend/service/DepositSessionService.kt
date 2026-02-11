package com.agent.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

data class DepositSession(
    val userId: Long,
    val walletAddress: String,
    val startedAt: Instant,
    val expiresAt: Instant
)

@Service
class DepositSessionService(
    private val redisTemplate: StringRedisTemplate
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val SESSION_PREFIX = "deposit-session:"
        private val SESSION_TTL = Duration.ofHours(24)
    }

    /**
     * Start a deposit session for a user
     */
    fun startSession(userId: Long, walletAddress: String): DepositSession {
        val key = "$SESSION_PREFIX$userId"
        val now = Instant.now()
        val expiresAt = now.plus(SESSION_TTL)

        redisTemplate.opsForValue().set(key, walletAddress, SESSION_TTL)
        logger.info { "[deposit-session] Started session for user $userId, expires at $expiresAt" }

        return DepositSession(
            userId = userId,
            walletAddress = walletAddress,
            startedAt = now,
            expiresAt = expiresAt
        )
    }

    /**
     * Check if user has an active deposit session
     */
    fun hasActiveSession(userId: Long): Boolean {
        val key = "$SESSION_PREFIX$userId"
        return redisTemplate.hasKey(key)
    }

    /**
     * Get active deposit session for user
     */
    fun getSession(userId: Long): DepositSession? {
        val key = "$SESSION_PREFIX$userId"
        val walletAddress = redisTemplate.opsForValue().get(key) ?: return null
        val ttl = redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS)

        if (ttl <= 0) return null

        val now = Instant.now()
        return DepositSession(
            userId = userId,
            walletAddress = walletAddress,
            startedAt = now.minusSeconds(SESSION_TTL.seconds - ttl),
            expiresAt = now.plusSeconds(ttl)
        )
    }

    /**
     * Get all active deposit sessions
     * Returns map of userId to walletAddress
     */
    fun getAllActiveSessions(): Map<Long, String> {
        val pattern = "$SESSION_PREFIX*"
        val keys = redisTemplate.keys(pattern)

        return keys.mapNotNull { key ->
            val userIdStr = key.removePrefix(SESSION_PREFIX)
            val userId = userIdStr.toLongOrNull() ?: return@mapNotNull null
            val walletAddress = redisTemplate.opsForValue().get(key) ?: return@mapNotNull null
            userId to walletAddress
        }.toMap()
    }

    /**
     * End deposit session for user
     */
    fun endSession(userId: Long) {
        val key = "$SESSION_PREFIX$userId"
        redisTemplate.delete(key)
        logger.info { "[deposit-session] Ended session for user $userId" }
    }
}
