package com.agent.backend.service

import com.agent.backend.db.entity.Asset
import com.agent.backend.db.entity.Notification
import com.agent.backend.db.entity.NotificationType
import com.agent.backend.db.entity.WalletTransaction
import com.agent.backend.db.rep.NotificationRepository
import com.agent.backend.db.rep.PriceTrackerRepository
import com.agent.backend.dto.AssetData
import com.agent.backend.dto.BalanceData
import com.agent.backend.dto.OrderData
import com.agent.backend.dto.SwapData
import com.agent.backend.dto.TransactionData
import com.agent.backend.dto.WalletStateMetadata
import com.agent.backend.dto.WalletStateResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

/**
 * Unified wallet state service with Redis caching and request deduplication.
 *
 * Architecture:
 * - Redis cache: 8-second TTL for user wallet state
 * - Request deduplication: Multiple concurrent requests wait for single computation
 * - Event-driven invalidation: Cache cleared on transaction/balance events
 * - Single endpoint: Replaces separate /balance, /assets, /transactions calls
 */
@Service
class WalletStateService(
    private val assetService: AssetService,
    private val walletService: WalletService,
    private val priceDataCache: PriceDataCacheService,
    private val orderService: OrderService,
    private val priceTrackerRepository: PriceTrackerRepository,
    private val notificationRepository: NotificationRepository,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    // Redis cache configuration
    private val CACHE_KEY_PREFIX = "wallet-state:"
    private val CACHE_TTL = Duration.ofSeconds(8)

    // Request deduplication map
    private val pendingRequests = ConcurrentHashMap<Long, CompletableDeferred<WalletStateResponse>>()

    /**
     * Internal cache structure with timestamp for Redis storage.
     */
    private data class CachedWalletState(
        val state: WalletStateResponse,
        val cachedAt: Long = System.currentTimeMillis()
    )

    /**
     * Get complete wallet state for a user with caching and deduplication.
     *
     * @param userId User ID
     * @param transactionsLimit Maximum number of transactions to return
     * @return Complete wallet state response
     */
    suspend fun getWalletState(userId: Long, transactionsLimit: Int = 20): WalletStateResponse {
        val cacheKey = getCacheKey(userId)

        // Try to get from Redis cache
        try {
            val cachedJson = redisTemplate.opsForValue().get(cacheKey)
            if (cachedJson != null) {
                val cached = objectMapper.readValue<CachedWalletState>(cachedJson)
                val cacheAge = System.currentTimeMillis() - cached.cachedAt
                logger.debug { "[wallet-state] Redis cache HIT for user $userId (age: ${cacheAge}ms)" }

                return cached.state.copy(
                    metadata = cached.state.metadata.copy(
                        fromCache = true,
                        cacheAge = cacheAge
                    )
                )
            }
        } catch (e: Exception) {
            logger.warn(e) { "[wallet-state] Error reading from Redis cache for user $userId" }
        }

        logger.debug { "[wallet-state] Redis cache MISS for user $userId, fetching data" }

        // Check if there's already a pending request for this user (deduplication)
        val existing = pendingRequests[userId]
        if (existing != null) {
            logger.debug { "[wallet-state] Request deduplication: waiting for existing request for user $userId" }
            return existing.await()
        }

        // Create new deferred for this request
        val deferred = CompletableDeferred<WalletStateResponse>()
        pendingRequests[userId] = deferred

        try {
            // Fetch fresh data
            val state = fetchWalletState(userId, transactionsLimit)

            // Cache the result in Redis
            try {
                val cached = CachedWalletState(state)
                val json = objectMapper.writeValueAsString(cached)
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL)
                logger.debug { "[wallet-state] Cached wallet state in Redis for user $userId (TTL: ${CACHE_TTL.seconds}s)" }
            } catch (e: Exception) {
                logger.error(e) { "[wallet-state] Error caching to Redis for user $userId" }
            }

            // Complete all waiting requests
            deferred.complete(state)

            return state
        } catch (e: Exception) {
            logger.error(e) { "[wallet-state] Error fetching wallet state for user $userId" }
            deferred.completeExceptionally(e)
            throw e
        } finally {
            // Clean up pending request
            pendingRequests.remove(userId)
        }
    }

    /**
     * Invalidate cache for a user (called on transaction/balance events).
     *
     * @param userId User ID
     * @param reason Reason for invalidation (for logging)
     */
    fun invalidateCache(userId: Long, reason: String) {
        val cacheKey = getCacheKey(userId)
        try {
            redisTemplate.delete(cacheKey)
            logger.debug { "[wallet-state] Redis cache invalidated for user $userId (reason: $reason)" }
        } catch (e: Exception) {
            logger.error(e) { "[wallet-state] Error invalidating Redis cache for user $userId" }
        }
    }

    /**
     * Get Redis cache key for a user.
     */
    private fun getCacheKey(userId: Long): String = "$CACHE_KEY_PREFIX$userId"

    /**
     * Fetch complete wallet state from database (uncached).
     *
     * @param userId User ID
     * @param transactionsLimit Maximum number of transactions to return
     * @return Fresh wallet state
     */
    private suspend fun fetchWalletState(userId: Long, transactionsLimit: Int): WalletStateResponse {
        val startTime = System.currentTimeMillis()

        // Fetch all data in parallel (assets and transactions are independent)
        val assets = assetService.list(userId)
        val transactions = walletService.getUserTransactionHistory(userId)
            .take(transactionsLimit)

        // Fetch swap notifications
        val swapNotifications = notificationRepository
            .findByUser_IdAndTypeOrderByCreatedAtDesc(userId, NotificationType.SWAP_EXECUTED)
            .take(transactionsLimit)

        // Fetch all orders for the user
        val allOrders = orderService.listAllOrdersByUser(userId)

        // Fetch all price trackers for the user (to avoid N+1 queries)
        val priceTrackers = priceTrackerRepository.findAllByUserId(userId)
            .associateBy { it.orderId }

        // Compute balance and enrich assets with prices (sequential due to rate limiting)
        val enrichedAssets = mutableListOf<AssetData>()
        for (asset in assets) {
            enrichedAssets.add(enrichAssetWithCachedPrices(asset))
        }
        val totalUsd = computeTotalBalance(enrichedAssets)

        // Map transactions to DTOs
        val transactionDtos = transactions.map { mapTransactionToDto(it) }

        // Map swap notifications to DTOs
        val swapDtos = swapNotifications.mapNotNull { mapSwapNotificationToDto(it) }

        // Map orders to DTOs and enrich with symbols
        val orderDtos = allOrders.map { mapOrderToDto(it, priceTrackers) }

        val fetchTime = System.currentTimeMillis() - startTime
        logger.debug { "[wallet-state] Fetched wallet state for user $userId in ${fetchTime}ms" }

        return WalletStateResponse(
            userId = userId,
            balance = BalanceData(
                totalUsd = totalUsd,
                lastUpdated = Instant.now()
            ),
            assets = enrichedAssets,
            transactions = transactionDtos,
            swaps = swapDtos,
            orders = orderDtos,
            metadata = WalletStateMetadata(
                fromCache = false,
                cacheAge = null,
                transactionCount = transactionDtos.size,
                transactionsLimit = transactionsLimit,
                swapCount = swapDtos.size,
                activeOrdersCount = orderDtos.count { !it.fulfilled },
                fulfilledOrdersCount = orderDtos.count { it.fulfilled }
            )
        )
    }

    /**
     * Enrich asset with price data using cached prices.
     *
     * Similar to BalanceService.enrichAsset() but uses PriceDataCacheService.
     */
    private suspend fun enrichAssetWithCachedPrices(asset: Asset): AssetData {
        // Get decimals
        val decimals = priceDataCache.getDecimals(asset.address)

        // Get unit price in USD
        val unitPrice = priceDataCache.getPriceUsd(asset.address)

        // Calculate readable amount
        val divisor = 10.0.pow(decimals.toDouble())
        val readableAmountValue = asset.amountNano.toDouble() / divisor
        val readableAmount = formatAmount(readableAmountValue)

        // Calculate total USD value
        val usdValue = readableAmountValue * unitPrice

        // Get symbol
        val symbol = priceDataCache.getSymbol(asset.address)

        return AssetData(
            id = asset.id!!,
            address = asset.address,
            amountNano = asset.amountNano,
            symbol = symbol,
            decimals = decimals,
            readableAmount = readableAmount,
            unitPrice = if (unitPrice > 0) unitPrice else null,
            usdValue = if (usdValue > 0) usdValue else null
        )
    }

    /**
     * Compute total balance in USD from enriched assets.
     */
    private fun computeTotalBalance(assets: List<AssetData>): Double {
        return assets.sumOf { it.usdValue ?: 0.0 }
    }

    /**
     * Map WalletTransaction entity to TransactionData DTO.
     */
    private fun mapTransactionToDto(tx: WalletTransaction): TransactionData {
        return TransactionData(
            id = tx.id!!,
            transactionHash = tx.transactionHash,
            transactionLt = tx.transactionLt,
            direction = tx.direction.name,
            amountNano = tx.amountNano,
            assetType = tx.assetType,
            jettonMasterAddress = tx.jettonMasterAddress,
            jettonSymbol = tx.jettonSymbol,
            jettonDecimals = tx.jettonDecimals,
            senderAddress = tx.senderAddress,
            recipientAddress = tx.recipientAddress,
            comment = tx.comment,
            createdAt = tx.createdAt
        )
    }

    /**
     * Map SWAP_EXECUTED notification to SwapData DTO.
     * Returns null if the notification metadata is malformed.
     */
    private fun mapSwapNotificationToDto(notification: Notification): SwapData? {
        val metadata = notification.metadata
        val fromAsset = metadata["fromAsset"] as? String ?: return null
        val toAsset = metadata["toAsset"] as? String ?: return null
        val fromAmount = metadata["fromAmount"]?.toString() ?: "unknown"
        val toAmount = metadata["toAmount"]?.toString() ?: "unknown"
        val transactionId = (metadata["transactionId"] as? String)?.takeIf { it.isNotEmpty() }

        return SwapData(
            id = notification.id!!,
            fromAsset = fromAsset,
            toAsset = toAsset,
            fromAmount = fromAmount,
            toAmount = toAmount,
            transactionId = transactionId,
            createdAt = notification.createdAt
        )
    }

    /**
     * Map Order entity to OrderData DTO.
     */
    private suspend fun mapOrderToDto(
        order: com.agent.backend.db.entity.Order,
        priceTrackers: Map<Long?, com.agent.backend.db.entity.PriceTracker>
    ): OrderData {
        // Try to get symbol from cache
        val symbol = try {
            priceDataCache.getSymbol(order.jettonMaster)
        } catch (e: Exception) {
            logger.debug(e) { "[wallet-state] Failed to get symbol for jetton ${order.jettonMaster}" }
            null
        }

        // Get price tracker from pre-loaded map
        val priceTracker = priceTrackers[order.id]

        return OrderData(
            id = order.id!!,
            jettonMaster = order.jettonMaster,
            action = order.action,
            amount = order.amount,
            createdAt = order.createdAt,
            fulfilled = order.fulfilled,
            symbol = symbol,
            targetPrice = priceTracker?.targetPrice,
            direction = priceTracker?.direction?.name
        )
    }

    /**
     * Format amount with appropriate precision based on value magnitude.
     *
     * Copied from BalanceService for consistency.
     */
    private fun formatAmount(amount: Double): String {
        return when {
            amount == 0.0 -> "0"
            amount >= 1.0 -> {
                BigDecimal(amount).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            }

            amount >= 0.01 -> {
                BigDecimal(amount).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            }

            else -> {
                BigDecimal(amount).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            }
        }
    }

    /**
     * Get cache statistics for monitoring.
     * Note: Redis doesn't provide built-in statistics like Caffeine,
     * so we return basic info about cache keys.
     */
    fun getCacheStats(): Map<String, Any> {
        return try {
            val pattern = "$CACHE_KEY_PREFIX*"
            val keys = redisTemplate.keys(pattern)
            mapOf(
                "cachedUsers" to (keys?.size ?: 0),
                "cacheKeyPattern" to pattern,
                "ttlSeconds" to CACHE_TTL.seconds
            )
        } catch (e: Exception) {
            logger.error(e) { "[wallet-state] Error getting cache stats" }
            mapOf(
                "error" to "Failed to get cache statistics",
                "ttlSeconds" to CACHE_TTL.seconds
            )
        }
    }
}
