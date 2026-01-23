package com.agent.backend.db.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "offline_tokens")
class OfflineToken(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "offline_token_id_seq")
    @SequenceGenerator(name = "offline_token_id_seq", sequenceName = "offline_token_id_seq", allocationSize = 1)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "refresh_token", columnDefinition = "text")
    var refreshToken: String? = null,

    @Column(name = "token_hash", columnDefinition = "text")
    var tokenHash: String,

    @Column(name = "client_id")
    var clientId: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    @Column(name = "encryption_key_id")
    var encryptionKeyId: String? = null
)
