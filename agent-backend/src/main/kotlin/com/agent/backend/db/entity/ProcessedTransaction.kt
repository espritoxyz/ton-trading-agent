package com.agent.backend.db.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "processed_transaction",
    indexes = [
        Index(name = "idx_processed_tx_body_hash", columnList = "body_hash", unique = true)
    ]
)
class ProcessedTransaction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "body_hash", nullable = false, unique = true, length = 64)
    var bodyHash: String,

    @Column(name = "transaction_lt", nullable = false)
    var transactionLt: Long,

    @Column(name = "transaction_hash", nullable = false, length = 64)
    var transactionHash: String,

    @Column(name = "deposit_request_id")
    var depositRequestId: Long? = null,

    @Column(name = "processed_at", nullable = false)
    var processedAt: Instant = Instant.now()
)
