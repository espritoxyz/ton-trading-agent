package com.agent.backend.service

import com.agent.backend.config.StonfiProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.text.Normalizer
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

@Service
class StonfiAssetsCacheService(
    private val stonfiProperties: StonfiProperties,
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Minimal view of STON.fi asset, containing only what we care about.
     */
    data class StonfiAsset(
        val contract_address: String,
        val symbol: String,
        val dex_usd_price: String?,
        val popularity_index: Double? = null,
        val decimals: Int? = null,
    )



    private data class AssetListResponse(
        val asset_list: List<StonfiAsset> = emptyList()
    )

    private val client: RestClient = RestClient.builder()
        .baseUrl(stonfiProperties.baseUrl)
        .build()

    /**
     * Indexed view for fast matching.
     */
    data class IndexedAsset(
        val contractAddress: String,
        val symbol: String,
        val normSymbol: String,
        val hasPrice: Boolean,
        val popularityIndex: Double? = null,
    ) {
        override fun toString() =
            "{jetton_master=$contractAddress,symbol=$symbol,norm_symbol=$normSymbol,has_price=$hasPrice,popularity_index=$popularityIndex}"
    }


    private val assetsRef: AtomicReference<List<StonfiAsset>> = AtomicReference(emptyList())
    private val indexedAssetsRef: AtomicReference<List<IndexedAsset>> = AtomicReference(emptyList())

    @Volatile
    private var lastUpdatedAt: Long = 0L

    init {
        try {
            refreshAssetsInternal()
        } catch (e: Exception) {
            logger.warn(e) { "[stonfi] Initial assets fetch failed" }
        }
    }

    @Scheduled(fixedDelayString = "30000")
    fun scheduledRefresh() {
        try {
            refreshAssetsInternal()
        } catch (e: Exception) {
            logger.warn(e) { "[stonfi] Scheduled assets refresh failed" }
        }
    }

    @Synchronized
    private fun refreshAssetsInternal() {
        val response = client
            .get()
            .uri { builder ->
                builder
                    .path("/v1/assets")
                    .queryParam("network", stonfiProperties.network)
                    .build()
            }
            .retrieve()
            .body(AssetListResponse::class.java)

        val assets = response?.asset_list ?: emptyList()
        assetsRef.set(assets)

        val indexed = assets.mapNotNull { a ->
            val norm = normalizeSymbol(a.symbol)
            if (norm.isEmpty()) null else IndexedAsset(
                contractAddress = a.contract_address,
                symbol = a.symbol,
                normSymbol = norm,
                hasPrice = !a.dex_usd_price.isNullOrBlank(),
                popularityIndex = a.popularity_index,
            )
        }

        indexedAssetsRef.set(indexed)

        lastUpdatedAt = System.currentTimeMillis()
    }

    fun getLastUpdatedAt(): Long = lastUpdatedAt
    fun getAllAssets(): List<StonfiAsset> = assetsRef.get()

    fun getAssetByContractAddress(contractAddress: String): StonfiAsset? {
        val addr = contractAddress.lowercase()
        return assetsRef.get().firstOrNull { it.contract_address.lowercase() == addr }
    }

    fun getDexUsdPrice(contractAddress: String): Double? =
        getAssetByContractAddress(contractAddress)?.dex_usd_price?.toDoubleOrNull()

    fun getDecimals(contractAddress: String): Int? =
        getAssetByContractAddress(contractAddress)?.decimals



    fun findCandidates(symbol: String, limit: Int = 50): List<IndexedAsset> {
        val q = normalizeSymbol(symbol)
        if (q.isEmpty()) return emptyList()

        val all = indexedAssetsRef.get()
        if (all.isEmpty()) return emptyList()

        val exact = all.asSequence()
            .filter { it.normSymbol == q }
            .take(limit)
            .toList()
        if (exact.isNotEmpty()) return exact

        data class Scored(val a: IndexedAsset, val score: Int)

        fun score(a: IndexedAsset): Int {
            val s = a.normSymbol
            var sc = 0

            // strong matches
            if (s == q) sc += 100
            if (s.startsWith(q)) sc += 40
            if (q.startsWith(s)) sc += 25
            if (s.contains(q)) sc += 15

            if (a.hasPrice) sc += 5

            // penalty for very different lengths (helps avoid junk)
            val lenDiff = abs(s.length - q.length)
            sc -= (lenDiff * 2).coerceAtMost(30)

            return sc
        }

        val scored = all.asSequence()
            .map { Scored(it, score(it)) }
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .take(limit)
            .map { it.a }
            .toList()

        return scored
    }

    /**
     * Normalization designed for ticker-like strings:
     * - uppercases
     * - strips $, spaces, separators
     * - removes diacritics
     * - keeps only A-Z0-9 (and optionally ':' if you want)
     */
    fun normalizeSymbol(input: String): String {
        if (input.isBlank()) return ""

        var s = input.trim().uppercase()

        // Unicode normalize + strip diacritics
        s = Normalizer.normalize(s, Normalizer.Form.NFKD)
            .replace(Regex("\\p{M}+"), "")

        // Remove common prefixes and separators
        s = s
            .replace("$", "")
            .replace(Regex("[\\s\\-._/\\\\]+"), "")

        // Keep only alnum (tickers are usually this)
        s = s.replace(Regex("[^A-Z0-9]"), "")

        return s
    }
}
