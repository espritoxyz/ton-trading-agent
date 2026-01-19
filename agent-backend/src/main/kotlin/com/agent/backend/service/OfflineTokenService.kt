package com.agent.backend.service

import com.agent.backend.db.entity.OfflineToken
import com.agent.backend.db.rep.OfflineTokenRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
open class OfflineTokenService(
    private val repo: OfflineTokenRepository,
    private val encryptionService: EncryptionService,
    private val tokenHashService: TokenHashService
) {
    /**
     * Сохраняет/обновляет offline token: вычисляет хэш и делает upsert по userId (+clientId если указан).
     * Внимание: refreshToken хранится зашифрованным временно (nullable), рекомендуется по возможности хранить только tokenHash.
     */
    @Transactional
    open fun saveForUser(userId: Long, refreshToken: String, clientId: String? = null, expiresAt: Instant? = null): OfflineToken {
        val encrypted = encryptionService.encrypt(refreshToken)
        val th = tokenHashService.hashToken(refreshToken)

        // ищем существующую запись: сначала по user+client, если client задан, иначе по user
        val existing = if (clientId != null) {
            // нет метода findByUserIdAndClientId в репозитории — используем findFirstByUserIdOrderByCreatedAtDesc и фильтрацию
            repo.findFirstByUserIdOrderByCreatedAtDesc(userId)?.takeIf { it.clientId == clientId }
        } else {
            repo.findFirstByUserIdOrderByCreatedAtDesc(userId)
        }

        if (existing != null) {
            existing.refreshToken = encrypted
            existing.tokenHash = th
            existing.clientId = clientId
            existing.expiresAt = expiresAt
            existing.lastUsedAt = Instant.now()
            existing.createdAt = Instant.now()
            return repo.save(existing)
        } else {
            val token = OfflineToken(userId = userId, refreshToken = encrypted, tokenHash = th, clientId = clientId, createdAt = Instant.now(), expiresAt = expiresAt)
            return repo.save(token)
        }
    }

    @Transactional(readOnly = true)
    open fun getLatestForUser(userId: Long): OfflineToken? {
        return repo.findFirstByUserIdOrderByCreatedAtDesc(userId)
    }

    @Transactional(readOnly = true)
    open fun getLatestDecryptedForUser(userId: Long): Pair<OfflineToken, String>? {
        val t = getLatestForUser(userId) ?: return null
        val decrypted = t.refreshToken?.let { encryptionService.decrypt(it) } ?: throw IllegalStateException("No refresh token stored for user")
        return Pair(t, decrypted)
    }

    @Transactional
    open fun revokeById(id: Long) {
        repo.deleteById(id)
    }

    @Transactional
    open fun revokeAllForUser(userId: Long) {
        val list = repo.findByUserId(userId)
        repo.deleteAll(list)
    }

    // Scheduled cleanup: удаляет просроченные записи каждый час
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    open fun cleanupExpiredTokens() {
        val now = Instant.now()
        repo.deleteByExpiresAtBefore(now)
    }
}
