package com.agent.backend.db.rep

import com.agent.backend.db.entity.TransactionDirection
import com.agent.backend.db.entity.WalletTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WalletTransactionRepository : JpaRepository<WalletTransaction, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<WalletTransaction>
    fun existsByTransactionHashAndDirection(hash: String, direction: TransactionDirection): Boolean
}
