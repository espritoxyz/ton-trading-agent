package com.agent.backend.service

import com.agent.backend.db.entity.AgentUser
import com.agent.backend.db.rep.AgentUserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val users: AgentUserRepository
) {
    fun getByIdOrThrow(id: Long): AgentUser =
        users.findById(id).orElseThrow { NoSuchElementException("User not found") }

    @Transactional
    fun updateEmail(id: Long, newEmail: String?): AgentUser {
        val u = getByIdOrThrow(id)
        u.email = newEmail
        return users.save(u)
    }
}
