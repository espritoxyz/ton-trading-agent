package com.agent.backend.db.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "price_tracker")
class PriceTracker(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "jetton_master", nullable = false)
    var jettonMaster: String,

    @Column(name = "target_price", nullable = false)
    var targetPrice: Double,

    @Column(name = "triggered", nullable = false)
    var triggered: Boolean = false,

    @Column(name = "order_id", nullable = true)
    var orderId: Long? = null,

    @Column(name = "direction", nullable = false)
    var direction: String, // "up" or "down"

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)
