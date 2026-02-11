package com.agent.backend.db.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "user_wallet")
class UserWallet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "wallet_address", nullable = false, length = 48, unique = true)
    var walletAddress: String,

    @Column(name = "encrypted_mnemonic", nullable = false, columnDefinition = "TEXT")
    var encryptedMnemonic: String,

    @Column(name = "encryption_key_id", nullable = false)
    var encryptionKeyId: String,

    @Column(nullable = false)
    var workchain: Int = 0,

    @Column(name = "wallet_version", nullable = false)
    var walletVersion: String = "V5R1",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
)
