package com.agent.backend.service

import com.agent.backend.config.StonfiProperties
import com.fasterxml.jackson.annotation.JsonProperty
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

    /** Map of known lowercase symbol -> jetton master address loaded from known_tokens.csv (if present on classpath). */
    private val knownTokens: Map<String, String> = loadKnownTokens()

    /**
     * Minimal view of STON.fi asset, containing only what we care about.
     */
    data class StonfiAsset(
        @JsonProperty("contract_address")
        val contractAddress: String,
        val symbol: String,
        @JsonProperty("dex_usd_price")
        private val dexUsdPriceString: String?,
        @JsonProperty("popularity_index")
        val popularityIndex: Double? = null,
        val decimals: Int,
    ) {
        val dexUsdPrice: Double?
            get() = dexUsdPriceString?.toDoubleOrNull()
    }

    private data class AssetListResponse(
        @JsonProperty("asset_list")
        val assetList: List<StonfiAsset> = emptyList()
    )

    private val client: RestClient = RestClient.builder()
        .baseUrl(stonfiProperties.baseUrl)
        .build()

    private fun loadKnownTokens(): Map<String, String> {
        return try {
            val resourceName = "known_tokens.csv"
            val stream = javaClass.classLoader.getResourceAsStream(resourceName)
            if (stream == null) {
                logger.warn { "[stonfi] Resource $resourceName not found on classpath, knownTokens map will be empty" }
                emptyMap()
            } else {
                stream.bufferedReader().useLines { linesSequence ->
                    linesSequence
                        .drop(1) // skip header
                        .mapNotNull { line ->
                            val parts = line.split(',')
                            if (parts.size < 2) return@mapNotNull null

                            val rawSymbol = parts[0].trim()
                            val jettonMaster = parts[1].trim()
                            val symbolKey = normalizeSymbol(rawSymbol)
                            if (symbolKey.isEmpty() || jettonMaster.isEmpty()) null else symbolKey to jettonMaster
                        }
                        .toMap()
                        .also { logger.info { "[stonfi] Loaded ${it.size} known tokens from $resourceName" } }
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("[stonfi] Failed to load known_tokens.csv", e)
        }
    }


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
            logger.error(e) { "[stonfi] Initial assets fetch failed" }
            throw e
        }
    }

    @Scheduled(fixedDelayString = "30000")
    fun scheduledRefresh() {
        try {
            refreshAssetsInternal()
        } catch (e: Exception) {
            logger.error(e) { "[stonfi] Scheduled assets refresh failed" }
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

        val assets = response?.assetList ?: throw RuntimeException("No asset list in response")
        assetsRef.set(assets)

        val indexed = assets.mapNotNull { a ->
            val norm = normalizeSymbol(a.symbol)
            if (norm.isEmpty()) null else IndexedAsset(
                contractAddress = a.contractAddress,
                symbol = a.symbol,
                normSymbol = norm,
                hasPrice = a.dexUsdPrice != null,
                popularityIndex = a.popularityIndex,
            )
        }

        indexedAssetsRef.set(indexed)

        lastUpdatedAt = System.currentTimeMillis()
    }

    fun getLastUpdatedAt(): Long = lastUpdatedAt
    fun getAllAssets(): List<StonfiAsset> = assetsRef.get()

    fun getAssetByContractAddress(contractAddress: String): StonfiAsset? {
        val addr = contractAddress.lowercase()
        return assetsRef.get().firstOrNull { it.contractAddress.lowercase() == addr }
    }


    fun findCandidates(symbol: String, limit: Int = 50): List<IndexedAsset> {
        val q = normalizeSymbol(symbol)
        if (q.isEmpty()) return emptyList()

        val all = indexedAssetsRef.get()
        if (all.isEmpty()) return emptyList()

        knownTokens[q]?.let { jettonMaster ->
            return@findCandidates all.find { it.contractAddress == jettonMaster}?.let { listOf(it) } ?: run {
                logger.error { "Known token $q was not found in stonfi assets cache" }
                emptyList()
            }
        }

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
     * - lowercases
     * - strips $, spaces, separators
     * - removes diacritics
     * - keeps only a-z0-9
     */
    fun normalizeSymbol(input: String): String {
        if (input.isBlank()) return ""

        var s = input.trim().lowercase()

        // Unicode normalize + strip diacritics
        s = Normalizer.normalize(s, Normalizer.Form.NFKD)
            .replace(Regex("\\p{M}+"), "")

        // Remove common prefixes and separators
        s = s
            .replace("$", "")
            .replace(Regex("[\\s\\-._/\\\\]+"), "")

        // Keep only lowercase alnum (tickers are usually this)
        s = s.replace(Regex("[^a-z0-9]"), "")

        return s
    }

}
