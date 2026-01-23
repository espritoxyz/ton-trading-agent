package com.agent.backend.service

import com.agent.backend.db.entity.OfflineToken
import com.agent.backend.db.rep.OfflineTokenRepository
import com.agent.backend.security.EncryptionService
import com.agent.backend.security.TokenHashService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val log = KotlinLogging.logger {}

@Service
class OfflineTokenService(
    private val repo: OfflineTokenRepository,
    private val tokenHashService: TokenHashService,
    private val encryptionService: EncryptionService,
    private val authService: AuthService
) {

    @Transactional
    fun saveForUser(
        userId: Long,
        refreshToken: String,
        clientId: String? = null,
        expiresAt: Instant? = null
    ): OfflineToken {
        val th = try {
            tokenHashService.hashToken(refreshToken)
        } catch (e: Exception) {
            log.error(e) { "Failed to hash token for user=$userId" }
            throw e
        }
        val (encrypted, keyId) = try {
            encryptionService.encrypt(refreshToken)
        } catch (e: Exception) {
            log.error(e) { "Failed to encrypt refresh token for user=$userId" }
            throw e
        }

        log.debug { "saveForUser: user=$userId client=$clientId tokenHash=$th keyId=$keyId" }

        val existing = if (clientId != null) {
            repo.findFirstByUserIdAndClientIdOrderByCreatedAtDesc(userId, clientId)
        } else {
            repo.findFirstByUserIdOrderByCreatedAtDesc(userId)
        }

        if (existing != null) {
            existing.tokenHash = th
            existing.refreshToken = encrypted
            existing.encryptionKeyId = keyId
            existing.clientId = clientId
            existing.expiresAt = expiresAt
            existing.lastUsedAt = Instant.now()
            val saved = repo.save(existing)
            log.debug { "Updated offline token id=${saved.id} for user=$userId" }
            return saved
        } else {
            val token = OfflineToken(
                userId = userId,
                refreshToken = encrypted,
                tokenHash = th,
                clientId = clientId,
                createdAt = Instant.now(),
                expiresAt = expiresAt,
                encryptionKeyId = keyId
            )
            val saved = repo.save(token)
            log.debug { "Inserted offline token id=${saved.id} for user=$userId" }
            return saved
        }
    }

    @Transactional(readOnly = true)
    fun getLatestForUser(userId: Long): OfflineToken? {
        return repo.findFirstByUserIdOrderByCreatedAtDesc(userId)
    }

    @Transactional
    fun refreshAccessForUser(userId: Long, clientId: String? = null): String? {
        val t = if (clientId != null) repo.findFirstByUserIdAndClientIdOrderByCreatedAtDesc(userId, clientId)
        else repo.findFirstByUserIdOrderByCreatedAtDesc(userId)

        if (t == null || t.refreshToken.isNullOrBlank()) return null

        // decrypt
        val refreshPlain = try {
            encryptionService.decrypt(t.refreshToken!!)
        } catch (e: Exception) {
            // cannot decrypt -> revoke
            repo.delete(t)
            return null
        }

        // call Keycloak to refresh
        val newTokens = try {
            authService.refreshWithRefreshToken(refreshPlain)
        } catch (e: Exception) {
            // refresh failed -> remove local token
            repo.delete(t)
            return null
        }

        val newRefresh = newTokens.refreshToken
        if (newRefresh != null) {
            val (enc, kid) = encryptionService.encrypt(newRefresh)
            t.refreshToken = enc
            t.encryptionKeyId = kid
            t.tokenHash = tokenHashService.hashToken(newRefresh)
            t.lastUsedAt = Instant.now()
            repo.save(t)
        }

        return newTokens.accessToken
    }

    @Transactional
    fun revokeById(id: Long) {
        repo.deleteById(id)
    }

    @Transactional
    fun revokeAllForUser(userId: Long) {
        val list = repo.findByUserId(userId)
        repo.deleteAll(list)
    }

    // Scheduled cleanup: удаляет просроченные записи каждый час
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    fun cleanupExpiredTokens() {
        val now = Instant.now()
        repo.deleteByExpiresAtBefore(now)
    }
}
