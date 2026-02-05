package com.agent.backend.db.rep

import com.agent.backend.db.entity.ProcessedTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProcessedTransactionRepository : JpaRepository<ProcessedTransaction, Long> {
    fun existsByBodyHash(bodyHash: String): Boolean
}
