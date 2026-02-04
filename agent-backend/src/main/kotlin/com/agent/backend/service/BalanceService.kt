package com.agent.backend.service

import com.agent.backend.db.entity.BalanceTransaction
import com.agent.backend.db.rep.BalanceTransactionRepository
import com.agent.backend.dto.BalanceResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import java.math.RoundingMode
import kotlin.math.pow

@Service
open class BalanceService(
    private val assetService: AssetService,
    private val txRepo: BalanceTransactionRepository,
    private val stonfiAssetsCache: StonfiAssetsCacheService
) {
    private val tonAddress = "EQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAM9c"

    private val binanceClient: RestClient = RestClient.builder()
        .baseUrl("https://api.binance.com/api/v3")
        .build()

    private data class TonToUsdtDto(
        val symbol: String,
        val price: Float,
    )
    fun getBalance(userId: Long): BalanceResponse {
        val assets = assetService.list(userId)

        val totalUsd = assets.sumOf { asset ->
            val decimals = if (asset.address.equals(tonAddress, ignoreCase = true)) {
                9 // Native TON uses 9 decimals
            } else {
                stonfiAssetsCache.getDecimals(asset.address) ?: 9 // Fallback to 9 if unknown
            }

            val divisor = 10.0.pow(decimals.toDouble())
            val units = asset.amountNano.toDouble() / divisor
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
        // Check if this is native TON
        if (address.equals(tonAddress, ignoreCase = true)) {
            return getTonToUSDT() ?: 1.0
        }

        // For jettons, get the price from STON.fi cache
        return stonfiAssetsCache.getDexUsdPrice(address) ?: 1.0
    }

    private fun getTonToUSDT(): Double? {
        return try {
            binanceClient
                .get()
                .uri("/ticker/price?symbol=TONUSDT")
                .retrieve()
                .body(TonToUsdtDto::class.java)?.price?.toBigDecimal()?.setScale(2, RoundingMode.HALF_UP)?.toDouble()
        } catch (e: Exception) {
            null
        }
    }
}
