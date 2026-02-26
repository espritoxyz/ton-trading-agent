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
@Table(name = "newsletter_subscription")
class NewsletterSubscription(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "newsletter_id_seq_gen")
    @SequenceGenerator(name = "newsletter_id_seq_gen", sequenceName = "newsletter_id_seq", allocationSize = 1)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var subscribedAt: Instant = Instant.now(),

    var unsubscribedAt: Instant? = null,

    @Column(nullable = false, unique = true)
    var unsubscribeToken: String,

    /** PENDING_VERIFICATION | ACTIVE | UNSUBSCRIBED */
    @Column(nullable = false)
    var status: String = "PENDING_VERIFICATION",

    var confirmedAt: Instant? = null,

    @Column(unique = true)
    var verificationToken: String? = null,

    var verificationTokenExpiresAt: Instant? = null,

    @Column(nullable = false)
    var resendCount: Int = 0,

    var lastResentAt: Instant? = null
)
