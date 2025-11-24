package com.agent.backend.service

import com.agent.backend.db.entity.Asset
import com.agent.backend.db.rep.AssetRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AssetService(
    private val assets: AssetRepository
) {
    fun list(userId: Long): List<Asset> =
        assets.findAllByUserId(userId)

    @Transactional
    fun upsertByAddress(userId: Long, address: String, amountNano: Long): Asset {
        val existing = assets.findByUserIdAndAddress(userId, address)
        return if (existing != null) {
            existing.amountNano = amountNano
            assets.save(existing)
        } else {
            assets.save(Asset(userId = userId, address = address, amountNano = amountNano))
        }
    }

    @Transactional
    fun deleteById(userId: Long, assetId: Long) {
        val a = assets.findById(assetId).orElse(null) ?: return
        require(a.userId == userId) { "forbidden" }
        assets.delete(a)
    }
}
