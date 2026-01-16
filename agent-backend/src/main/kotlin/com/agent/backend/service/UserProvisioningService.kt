package com.agent.backend.service

import com.agent.backend.db.entity.AgentUser
import com.agent.backend.db.rep.AgentUserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
open class UserProvisioningService(
    private val users: AgentUserRepository
) {
    @Transactional
    open fun resolveOrCreate(subject: String, email: String?): AgentUser {
        val existing = users.findBySubject(subject).orElse(null)

        if (existing != null) {
            existing.lastLoginAt = Instant.now()
            if (!email.isNullOrBlank()) existing.email = email
            return users.save(existing)
        }

        return users.save(
            AgentUser(
                subject = subject,
                email = email,
                createdAt = Instant.now(),
                lastLoginAt = Instant.now()
            )
        )
    }

    @Transactional
    open fun createLocalForKeycloak(subject: String, email: String?): AgentUser {
        // throw if exists
        val existing = users.findBySubject(subject).orElse(null)
        if (existing != null) throw IllegalStateException("user already exists locally for subject")

        val u = AgentUser(
            subject = subject,
            email = email,
            createdAt = Instant.now(),
            lastLoginAt = Instant.now()
        )
        return users.save(u)
    }
}
