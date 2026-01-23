package com.agent.backend.db.rep

import com.agent.backend.db.entity.OfflineToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface OfflineTokenRepository : JpaRepository<OfflineToken, Long> {
    fun findByUserId(userId: Long): List<OfflineToken>
    fun findFirstByUserIdOrderByCreatedAtDesc(userId: Long): OfflineToken?
    fun findFirstByUserIdAndClientIdOrderByCreatedAtDesc(userId: Long, clientId: String): OfflineToken?
    fun findByTokenHash(tokenHash: String): List<OfflineToken>
    fun deleteByExpiresAtBefore(instant: Instant): Int
}
