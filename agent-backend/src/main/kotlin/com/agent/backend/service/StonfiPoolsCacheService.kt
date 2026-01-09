package com.agent.backend.service

import com.agent.backend.config.StonfiProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.concurrent.atomic.AtomicReference

@Service
class StonfiPoolsCacheService(
    private val stonfiProperties: StonfiProperties,
) {

    private val tonAddress = "EQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAM9c"

    private val logger = KotlinLogging.logger {}

    data class StonfiPool(
        val address: String,
        val router_address: String,
        val token0_address: String,
        val token1_address: String,
        val token0_balance: String? = null,
        val token1_balance: String? = null,
        val reserve0: String? = null,
        val reserve1: String? = null,
        val deprecated: Boolean? = null,
    )

    private data class PoolListResponse(
        val pool_list: List<StonfiPool> = emptyList()
    )

    private val client: RestClient = RestClient.builder()
        .baseUrl(stonfiProperties.baseUrl)
        .build()

    private val poolsRef: AtomicReference<List<StonfiPool>> = AtomicReference(emptyList())

    @Volatile
    private var lastUpdatedAt: Long = 0L

    init {
        // Trigger initial load on startup, but don't fail the app if it errors
        try {
            refreshPoolsInternal()
        } catch (e: Exception) {
            logger.warn(e) { "[stonfi] Initial pools fetch failed" }
        }
    }

    @Scheduled(fixedDelayString = "30000")
    fun scheduledRefresh() {

        try {
            refreshPoolsInternal()
        } catch (e: Exception) {
            logger.warn(e) { "[stonfi] Scheduled pools refresh failed" }
        }
    }

    @Synchronized
    private fun refreshPoolsInternal() {
        logger.debug { "[stonfi] Refreshing pools from STON.fi" }

        val response = client
            .get()
            .uri { builder ->
                builder
                    .path("/v1/pools")
                    .queryParam("network", stonfiProperties.network)
                    .build()
            }
            .retrieve()
            .body(PoolListResponse::class.java)

        val pools = response?.pool_list ?: emptyList()
        poolsRef.set(pools)
        lastUpdatedAt = System.currentTimeMillis()

        logger.info { "[stonfi] Pools updated: count=${pools.size}, network=${stonfiProperties.network}" }
    }

    fun getLastUpdatedAt(): Long = lastUpdatedAt

    fun getAllPools(): List<StonfiPool> = poolsRef.get()

    fun getPoolByAddress(poolAddress: String): StonfiPool? {
        val addr = poolAddress.lowercase()
        return poolsRef.get().firstOrNull { it.address.lowercase() == addr }
    }

    /**

     * Returns the "best" pool for the given token pair, mimicking pickBestStonfiPoolForPair logic,
     * but using only local cached pool data and geometric-mean score.
     */
    fun getBestPoolByTokens(tokenA: String, tokenB: String): StonfiPool? {

        val a = tokenA.lowercase()
        val b = tokenB.lowercase()

        var best: StonfiPool? = null
        var bestScore: java.math.BigInteger = java.math.BigInteger.valueOf(-1)

        for (p in poolsRef.get()) {
            if (p.deprecated == true) continue

            val t0 = p.token0_address.lowercase()
            val t1 = p.token1_address.lowercase()

            val orientation: Int = when {
                t0 == a && t1 == b -> 0
                t0 == b && t1 == a -> 1
                else -> continue
            }

            val r0 = parseBig(p.token0_balance ?: p.reserve0) ?: continue
            val r1 = parseBig(p.token1_balance ?: p.reserve1) ?: continue

            if (r0 <= java.math.BigInteger.ZERO || r1 <= java.math.BigInteger.ZERO) continue

            val reserveA = if (orientation == 0) r0 else r1
            val reserveB = if (orientation == 0) r1 else r0

            val score = gmScore(reserveA, reserveB)
            if (score > bestScore) {
                bestScore = score
                best = p
            }
        }

        return best
    }

    /**
     * Convenience for "token vs TON" where TON jetton master is fixed.
     * tokenJettonMaster should already be normalized; comparison is done in lowercase.
     */
    fun getBestPoolByTokenAndTon(tokenJettonMaster: String): StonfiPool? {
        val tonJettonMaster = "EQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAM9c"
        return getBestPoolByTokens(tokenJettonMaster, tonJettonMaster)
    }

    private fun parseBig(v: String?): java.math.BigInteger? {

        if (v.isNullOrBlank()) return null
        return try {
            java.math.BigInteger(v)
        } catch (e: Exception) {
            null
        }
    }

    private fun gmScore(a: java.math.BigInteger, b: java.math.BigInteger): java.math.BigInteger {
        val prod = a.multiply(b)
        if (prod <= java.math.BigInteger.ZERO) return java.math.BigInteger.ZERO

        // integer sqrt via Newton method
        var x0 = prod
        var x1 = prod.shiftRight(1).add(java.math.BigInteger.ONE)
        while (x1 < x0) {
            x0 = x1
            x1 = x1.add(prod.divide(x1)).shiftRight(1)
        }
        return x0
    }
}