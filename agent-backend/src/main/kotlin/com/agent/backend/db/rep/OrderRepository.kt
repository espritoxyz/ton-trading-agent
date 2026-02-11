package com.agent.backend.db.rep

import com.agent.backend.db.entity.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    fun findAllByUserId(userId: Long): List<Order>
}
