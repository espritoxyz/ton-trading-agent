package com.agent.backend.service

import com.agent.backend.db.entity.AgentUser
import com.agent.backend.db.rep.AgentUserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class UserProvisioningService(
    private val users: AgentUserRepository
) {
    @Transactional
    fun resolveOrCreate(issuer: String, subject: String, email: String?): AgentUser {
        val existing = users.findByIssuerAndSubject(issuer, subject).orElse(null)

        if (existing != null) {
            existing.lastLoginAt = Instant.now()
            if (!email.isNullOrBlank()) existing.email = email
            return users.save(existing)
        }

        return users.save(
            AgentUser(
                issuer = issuer,
                subject = subject,
                email = email,
                createdAt = Instant.now(),
                lastLoginAt = Instant.now()
            )
        )
    }
}
