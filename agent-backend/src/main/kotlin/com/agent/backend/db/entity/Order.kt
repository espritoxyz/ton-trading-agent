package com.agent.backend.db.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
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

    @Column(name = "received_jetton_master", nullable = false)
    var receivedJettonMaster: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "fulfilled", nullable = false)
    var fulfilled: Boolean = false,
)
