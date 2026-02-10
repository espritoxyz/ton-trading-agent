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
@Table(
    name = "agent_user",
)
class AgentUser(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_seq_gen")
    @SequenceGenerator(name = "user_id_seq_gen", sequenceName = "user_id_seq", allocationSize = 1)
    var id: Long? = null,

    @Column(nullable = false) var subject: String,

    var email: String? = null,

    @Column(nullable = false) var emailVerified: Boolean = false,

    var emailVerificationSentAt: Instant? = null,

    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    var lastLoginAt: Instant? = null
)
