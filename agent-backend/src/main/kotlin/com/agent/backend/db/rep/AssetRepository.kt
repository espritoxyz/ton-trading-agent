package com.agent.backend.db.rep

import com.agent.backend.db.entity.Asset
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AssetRepository : JpaRepository<Asset, Long> {
    fun findAllByUserId(userId: Long): List<Asset>
    fun findByUserIdAndAddress(userId: Long, address: String): Asset?
}
