package com.agent.backend.service

import com.agent.backend.config.StonfiProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicReference
import org.springframework.beans.factory.annotation.Value


@Service
class StonfiPoolsCacheService(
    private val stonfiProperties: StonfiProperties,
    private val stonfiAssetsCacheService: StonfiAssetsCacheService,
    @Value("\${addressbook.ton}")
    private val tonAddress: String,
    @Value("\${addressbook.usdt}")
    private val usdtAddress: String,
) {

    private val MIN_TVL_USD: BigDecimal = BigDecimal("20000")

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

    class LowTvlPoolException(message: String) : RuntimeException(message)
    class NoSupportedPoolException(message: String) : RuntimeException(message)


    private data class PoolListResponse(
        @JsonProperty("pool_list")
        val poolList: List<StonfiPool> = emptyList()
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

        val pools = response?.poolList ?: emptyList()
        poolsRef.set(pools)
        lastUpdatedAt = System.currentTimeMillis()
    }

    fun getLastUpdatedAt(): Long = lastUpdatedAt

    fun getAllPools(): List<StonfiPool> = poolsRef.get()

    fun getPoolByAddress(poolAddress: String): StonfiPool? {
        val addr = poolAddress.lowercase()
        return poolsRef.get().firstOrNull { it.address.lowercase() == addr }
    }

    /**

     * Returns the "best" pool for the given token pair using a TVL-based score.
     * Pools with TVL < MIN_TVL_USD are considered unsafe: if at least one such pool
     * exists for the pair but none meet the threshold, a LowTvlPoolException is thrown.
     */
    fun getBestPoolByTokens(tokenA: String, tokenB: String): StonfiPool? {
        val a = tokenA.lowercase()
        val b = tokenB.lowercase()

        // We only support pools where at least one side is TON or USDT
        val ton = tonAddress.lowercase()
        val usdt = usdtAddress.lowercase()

        if (!(a == ton || b == ton || a == usdt || b == usdt)) {
            throw NoSupportedPoolException("Pools without TON or USDT are not supported: $a-$b")
        }

        var best: StonfiPool? = null

        var bestTvl: BigDecimal = BigDecimal.ZERO
        var hasAnyBelowThreshold = false

        for (p in poolsRef.get()) {
            if (p.deprecated == true) continue

            val t0 = p.token0_address.lowercase()
            val t1 = p.token1_address.lowercase()

            if (!((t0 == a && t1 == b) || (t0 == b && t1 == a))) {
                continue
            }

            val r0 = parseBig(p.token0_balance ?: p.reserve0) ?: continue
            val r1 = parseBig(p.token1_balance ?: p.reserve1) ?: continue

            if (r0 <= BigInteger.ZERO || r1 <= BigInteger.ZERO) {
                hasAnyBelowThreshold = true
                continue
            }

            val price0 = stonfiAssetsCacheService.getDexUsdPrice(p.token0_address)
            val price1 = stonfiAssetsCacheService.getDexUsdPrice(p.token1_address)
            if (price0 == null || price1 == null) {
                continue
            }

            val tvl0 = BigDecimal(r0).multiply(BigDecimal.valueOf(price0))
            val tvl1 = BigDecimal(r1).multiply(BigDecimal.valueOf(price1))
            val tvl = tvl0.add(tvl1)

            if (tvl <= MIN_TVL_USD) {
                hasAnyBelowThreshold = true
                continue
            }

            if (tvl > bestTvl) {
                bestTvl = tvl
                best = p
            }
        }


        if (best != null) {
            logger.debug { "Picked best pool for $tokenA-$tokenB \n$best" }
            return best
        }

        if (hasAnyBelowThreshold) {
            throw LowTvlPoolException("All pools for pair $a-$b have TVL below ${MIN_TVL_USD.toPlainString()} USD")
        }

        return null
    }


    /**
     * Convenience for "token vs TON" where TON jetton master is fixed.
     * tokenJettonMaster should already be normalized; comparison is done in lowercase.
     */
    fun getBestPoolByTokenAndTon(tokenJettonMaster: String): StonfiPool? {
        return getBestPoolByTokens(tokenJettonMaster, tonAddress)
    }

    private fun parseBig(v: String?): BigInteger? {
        if (v.isNullOrBlank()) return null
        return runCatching {
            BigInteger(v)
        }.getOrNull()
    }

}
