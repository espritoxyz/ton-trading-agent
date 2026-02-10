package com.agent.backend.db.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "email_verification_tokens")
class EmailVerificationToken(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "email_verification_token_id_seq_gen")
    @SequenceGenerator(
        name = "email_verification_token_id_seq_gen",
        sequenceName = "email_verification_token_id_seq",
        allocationSize = 1
    )
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(nullable = false, unique = true)
    var token: String,

    @Column(nullable = false)
    var email: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "verified_at")
    var verifiedAt: Instant? = null,

    @Column(nullable = false)
    var attempts: Int = 0,

    @Column(name = "last_resent_at")
    var lastResentAt: Instant? = null,

    @Column(name = "resend_count", nullable = false)
    var resendCount: Int = 0
) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    fun isVerified(): Boolean = verifiedAt != null

    fun isActive(): Boolean = !isExpired() && !isVerified()
}
