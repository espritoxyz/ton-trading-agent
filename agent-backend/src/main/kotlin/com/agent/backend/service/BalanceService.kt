package com.agent.backend.service

import com.agent.backend.db.entity.Asset
import com.agent.backend.db.entity.BalanceTransaction
import com.agent.backend.db.rep.BalanceTransactionRepository
import com.agent.backend.dto.AssetResponse
import com.agent.backend.dto.BalanceResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import java.math.BigDecimal
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
            val decimals = if (asset.address.equals(tonAddress, ignoreCase = true) || asset.address.equals("TON", ignoreCase = true)) {
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
        // Check if this is native TON (supports both "TON" and full address)
        if (address.equals(tonAddress, ignoreCase = true) || address.equals("TON", ignoreCase = true)) {
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

    /**
     * Enriches asset with price data, decimals, and metadata.
     */
    fun enrichAsset(asset: Asset): AssetResponse {
        val isTon = asset.address.equals(tonAddress, ignoreCase = true) || asset.address.equals("TON", ignoreCase = true)

        // Get decimals
        val decimals = if (isTon) {
            9
        } else {
            stonfiAssetsCache.getDecimals(asset.address) ?: 9
        }

        // Get unit price in USD
        val unitPrice = priceUsdPerUnit(asset.address)

        // Calculate readable amount
        val divisor = 10.0.pow(decimals.toDouble())
        val readableAmountValue = asset.amountNano.toDouble() / divisor
        val readableAmount = formatAmount(readableAmountValue)

        // Calculate total USD value
        val usdValue = readableAmountValue * unitPrice

        // Get symbol from STON.fi cache for jettons, or use "TON" for native token
        val symbol = if (isTon) {
            "TON"
        } else {
            stonfiAssetsCache.getAssetByContractAddress(asset.address)?.symbol
        }

        return AssetResponse(
            id = asset.id!!,
            address = asset.address,
            amountNano = asset.amountNano,
            symbol = symbol,
            decimals = decimals,
            name = null, // Could be added from STON.fi if needed
            imageUrl = null, // Could be added from TonAPI if needed
            readableAmount = readableAmount,
            unitPrice = if (unitPrice > 0) unitPrice else null,
            usdValue = if (usdValue > 0) usdValue else null
        )
    }

    /**
     * Formats amount with appropriate precision based on value magnitude.
     */
    private fun formatAmount(amount: Double): String {
        return when {
            amount == 0.0 -> "0"
            amount >= 1.0 -> {
                // For amounts >= 1, show 2-4 decimal places
                BigDecimal(amount).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            }
            amount >= 0.01 -> {
                // For amounts >= 0.01, show up to 4 decimals
                BigDecimal(amount).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            }
            else -> {
                // For very small amounts, show up to 8 decimals
                BigDecimal(amount).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            }
        }
    }
}
