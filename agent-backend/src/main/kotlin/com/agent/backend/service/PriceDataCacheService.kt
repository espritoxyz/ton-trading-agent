package com.agent.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.math.RoundingMode
import java.time.Duration

/**
 * Caches price data for TON and jettons with rate limiting and Redis persistence.
 *
 * Architecture:
 * - TON/USDT price: Redis cache (45s TTL), rate-limited Binance API (1 req/sec)
 * - Jetton prices: Passthrough to STON.fi cache (30s in-memory refresh)
 */
@Service
class PriceDataCacheService(
    private val redisTemplate: StringRedisTemplate,
    private val stonfiAssetsCache: StonfiAssetsCacheService,
    @Value("\${addressbook.ton}")
    private val tonAddress: String,
) {
    private val logger = KotlinLogging.logger {}

    // Redis cache keys
    private val TON_USDT_PRICE_KEY = "price:ton:usdt"
    private val TON_USDT_TTL = Duration.ofSeconds(45)

    // Simple rate limiter: 1 request per second for Binance API
    private var lastBinanceCallMs = 0L
    private val binanceRateLimitMutex = Mutex()
    private val minIntervalMs = 1000L

    // Binance API client
    private val binanceClient: RestClient = RestClient.builder()
        .baseUrl("https://api.binance.com/api/v3")
        .build()

    private data class TonToUsdtDto(
        val symbol: String,
        val price: Float,
    )

    /**
     * Get price in USD for any asset (TON or jetton).
     *
     * @param address Asset address (TON address or jetton master address)
     * @return Price in USD, or 1.0 as fallback
     */
    suspend fun getPriceUsd(address: String): Double {
        return if (address.equals(tonAddress, ignoreCase = true) || address.equals("TON", ignoreCase = true)) {
            getTonUsdtPrice()
        } else {
            getJettonPrice(address)
        }
    }

    /**
     * Get TON/USDT price from Redis cache or Binance API.
     * Uses rate limiting to prevent exceeding Binance API limits.
     *
     * @return TON price in USDT, or 1.0 as fallback
     */
    suspend fun getTonUsdtPrice(): Double {
        try {
            // Try to get from Redis cache
            val cached = redisTemplate.opsForValue().get(TON_USDT_PRICE_KEY)
            if (cached != null) {
                logger.debug { "[price-cache] TON/USDT from Redis: $cached" }
                return cached.toDoubleOrNull() ?: 1.0
            }

            // Cache miss - fetch from Binance with rate limiting
            acquireBinanceRateLimit()
            logger.debug { "[price-cache] Fetching TON/USDT from Binance (rate-limited)" }

            val price = fetchTonUsdtFromBinance()
            if (price != null) {
                // Store in Redis with TTL
                redisTemplate.opsForValue().set(TON_USDT_PRICE_KEY, price.toString(), TON_USDT_TTL)
                logger.debug { "[price-cache] Cached TON/USDT in Redis: $price (TTL: ${TON_USDT_TTL.seconds}s)" }
                return price
            }

            // Fallback if API call fails
            logger.warn { "[price-cache] Failed to fetch TON/USDT from Binance, using fallback" }
            return 1.0
        } catch (e: Exception) {
            logger.error(e) { "[price-cache] Error fetching TON/USDT price" }
            return 1.0
        }
    }

    /**
     * Simple rate limiter: ensures minimum 1 second between Binance API calls.
     */
    private suspend fun acquireBinanceRateLimit() {
        binanceRateLimitMutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastBinanceCallMs

            if (elapsed < minIntervalMs) {
                val waitTime = minIntervalMs - elapsed
                logger.debug { "[price-cache] Rate limit: waiting ${waitTime}ms before Binance call" }
                delay(waitTime)
            }

            lastBinanceCallMs = System.currentTimeMillis()
        }
    }

    /**
     * Fetch TON/USDT price directly from Binance API.
     *
     * @return Price or null if API call fails
     */
    private fun fetchTonUsdtFromBinance(): Double? {
        return try {
            binanceClient
                .get()
                .uri("/ticker/price?symbol=TONUSDT")
                .retrieve()
                .body(TonToUsdtDto::class.java)
                ?.price
                ?.toBigDecimal()
                ?.setScale(2, RoundingMode.HALF_UP)
                ?.toDouble()
        } catch (e: Exception) {
            logger.warn(e) { "[price-cache] Binance API call failed" }
            null
        }
    }

    /**
     * Get jetton price from STON.fi cache.
     *
     * @param contractAddress Jetton master contract address
     * @return Price in USD, or 1.0 as fallback
     */
    fun getJettonPrice(contractAddress: String): Double {
        return stonfiAssetsCache.getAssetByContractAddress(contractAddress)?.dexUsdPrice ?: 1.0
    }

    /**
     * Get asset decimals (TON or jetton).
     *
     * @param address Asset address
     * @return Number of decimals, defaults to 9 for TON or unknown jettons
     */
    fun getDecimals(address: String): Int {
        return if (address.equals(tonAddress, ignoreCase = true) || address.equals("TON", ignoreCase = true)) {
            9
        } else {
            stonfiAssetsCache.getAssetByContractAddress(address)?.decimals ?: 9
        }
    }

    /**
     * Get asset symbol (TON or jetton).
     *
     * @param address Asset address
     * @return Symbol or null if not found
     */
    fun getSymbol(address: String): String? {
        return if (address.equals(tonAddress, ignoreCase = true) || address.equals("TON", ignoreCase = true)) {
            "TON"
        } else {
            stonfiAssetsCache.getAssetByContractAddress(address)?.symbol
        }
    }
}
