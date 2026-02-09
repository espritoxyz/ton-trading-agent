package com.agent.backend.db.rep

import com.agent.backend.db.entity.EmailVerificationToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface EmailVerificationTokenRepository : JpaRepository<EmailVerificationToken, Long> {
    fun findByToken(token: String): Optional<EmailVerificationToken>

    @Query(
        """
        SELECT t FROM EmailVerificationToken t
        WHERE t.userId = :userId
        AND t.verifiedAt IS NULL
        AND t.expiresAt > :now
        """
    )
    fun findActiveByUserId(userId: Long, now: Instant = Instant.now()): Optional<EmailVerificationToken>

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now")
    fun deleteExpiredTokens(now: Instant): Int
}
