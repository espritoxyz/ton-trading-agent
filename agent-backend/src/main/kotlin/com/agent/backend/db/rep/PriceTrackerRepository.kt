package com.agent.backend.db.rep

import com.agent.backend.db.entity.PriceTracker
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PriceTrackerRepository : JpaRepository<PriceTracker, Long> {
    fun findAllByUserId(userId: Long): List<PriceTracker>
    fun findAllByTriggeredFalse(): List<PriceTracker>
    fun findByOrderId(orderId: Long): PriceTracker?
}
