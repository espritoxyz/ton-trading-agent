package com.agent.backend.db.rep

import com.agent.backend.db.entity.BalanceTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BalanceTransactionRepository : JpaRepository<BalanceTransaction, Long> {
    fun findAllByUserId(userId: Long): List<BalanceTransaction>
}
