package com.agent.backend.db.rep

import com.agent.backend.db.entity.AgentUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AgentUserRepository : JpaRepository<AgentUser, Long> {
    fun findBySubject(subject: String): Optional<AgentUser>
    fun findByEmail(email: String): Optional<AgentUser>
}
