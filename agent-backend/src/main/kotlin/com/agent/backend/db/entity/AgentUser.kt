package com.agent.backend.db.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "agent_user",
    uniqueConstraints = [UniqueConstraint(name = "uq_agent_user_identity", columnNames = ["issuer", "subject"])]
)
class AgentUser(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_seq_gen")
    @SequenceGenerator(name = "user_id_seq_gen", sequenceName = "user_id_seq", allocationSize = 1)
    var id: Long? = null,

    @Column(nullable = false) var issuer: String,
    @Column(nullable = false) var subject: String,

    var email: String? = null,

    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    var lastLoginAt: Instant? = null
)
