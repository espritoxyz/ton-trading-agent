package com.agent.backend.db.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "jetton_master", nullable = false)
    var jettonMaster: String,

    // buy/sell
    @Column(name = "action", nullable = false)
    var action: String,

    @Column(name = "amount", nullable = false)
    var amount: Double,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "fulfilled", nullable = false)
    var fulfilled: Boolean = false,
)
