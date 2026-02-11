package com.agent.backend.db.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "asset",
    uniqueConstraints = [UniqueConstraint(name = "uq_asset_user_address", columnNames = ["user_id", "address"])]
)
class Asset(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(nullable = false)
    var address: String,

    @Column(name = "amount_nano", nullable = false)
    var amountNano: Long,
)
