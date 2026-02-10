package com.agent.backend.db.rep

import com.agent.backend.db.entity.DepositRequest
import com.agent.backend.db.entity.DepositStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DepositRequestRepository : JpaRepository<DepositRequest, Long> {
    fun findByCodeAndStatus(code: String, status: DepositStatus): DepositRequest?
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<DepositRequest>
    fun findAllByStatusAndExpiresAtBefore(status: DepositStatus, expiresAt: java.time.Instant): List<DepositRequest>
    fun findFirstByUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
        userId: Long,
        status: DepositStatus,
        expiresAt: java.time.Instant
    ): DepositRequest?
}
