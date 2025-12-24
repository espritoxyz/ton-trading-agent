package com.agent.backend.service

import com.agent.backend.db.entity.BalanceTransaction
import com.agent.backend.db.rep.BalanceTransactionRepository
import com.agent.backend.dto.BalanceResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class BalanceService(
    private val assetService: AssetService,
    private val txRepo: BalanceTransactionRepository
) {
    fun getBalance(userId: Long): BalanceResponse {
        val assets = assetService.list(userId)

        val totalUsd = assets.sumOf { asset ->
            val units = asset.amountNano.toDouble() / 1_000_000_000.0
            val price = priceUsdPerUnit(asset.address)
            units * price
        }

        return BalanceResponse(userId = userId, totalUsd = totalUsd)
    }

    @Transactional
    open fun recordTransaction(userId: Long, type: String, amountUsd: Double, reference: String? = null) : BalanceTransaction {
        val cents = kotlin.math.round(amountUsd * 100).toLong()
        val tx = BalanceTransaction(userId = userId, type = type, amountUsdCents = cents, reference = reference)
        return txRepo.save(tx)
    }

    private fun priceUsdPerUnit(address: String): Double {
        // TODO: implement real price lookup
        return 1.0
    }
}
