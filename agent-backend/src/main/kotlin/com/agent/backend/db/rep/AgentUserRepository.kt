package com.agent.backend.db.rep

import com.agent.backend.db.entity.AgentUser
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*
import org.springframework.stereotype.Repository

@Repository
interface AgentUserRepository : JpaRepository<AgentUser, Long> {
    fun findByIssuerAndSubject(issuer: String, subject: String): Optional<AgentUser>
}
