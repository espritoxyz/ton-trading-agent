package com.agent.backend.db.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "balance_transaction")
class BalanceTransaction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(nullable = false)
    var type: String, // e.g. CREDIT, DEBIT

    @Column(nullable = false)
    var amountUsdCents: Long,

    var reference: String? = null,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now()
)
