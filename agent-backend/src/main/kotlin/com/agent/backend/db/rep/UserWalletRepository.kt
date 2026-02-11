package com.agent.backend.db.rep

import com.agent.backend.db.entity.UserWallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserWalletRepository : JpaRepository<UserWallet, Long> {
    fun findByUserId(userId: Long): Optional<UserWallet>
    fun findByWalletAddress(address: String): Optional<UserWallet>
    fun findAllByIsActive(isActive: Boolean): List<UserWallet>
}
