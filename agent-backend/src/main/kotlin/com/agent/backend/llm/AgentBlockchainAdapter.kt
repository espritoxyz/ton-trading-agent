package com.agent.backend.llm

import com.agent.backend.rabbitmq.RabbitConfig
import com.agent.backend.service.ExternalToolResultService
import com.agent.backend.service.OrderService
import com.agent.backend.service.PriceTrackerService
import com.agent.backend.service.StonfiAssetsCacheService
import com.agent.backend.service.StonfiPoolsCacheService
import com.agent.backend.service.WalletService
import com.agent.llm.tool.api.BlockchainAdapter
import com.explyt.ai.dto.ToolResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger {}

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class AgentBlockchainAdapter(
    userId: Long,
    private val rabbitTemplate: RabbitTemplate,
    private var messageId: UUID,
    private val poolsCache: StonfiPoolsCacheService,
    private val assetsCache: StonfiAssetsCacheService,
    private val walletService: WalletService,
    private val priceTrackerService: PriceTrackerService,
    private val orderService: OrderService,
    private val externalToolResultService: ExternalToolResultService,
    private val walletService: WalletService,
) : BlockchainAdapter(userId) {

    companion object {
        private val staticBinanceClient: RestClient = RestClient.builder()
            .baseUrl("https://api.binance.com/api/v3")
            .build()

        private data class TonToUsdtDtoStatic(
            val symbol: String,
            val price: Float,
        )

        fun getTonToUSDTStatic(): Double? {
            return staticBinanceClient
                .get()
                .uri("/ticker/price?symbol=TONUSDT")
                .retrieve()
                .body<TonToUsdtDtoStatic>()
                ?.price
                ?.toBigDecimal()
                ?.setScale(2, RoundingMode.HALF_UP)
                ?.toDouble()
        }
    }

    override fun updateCurrentMessageId(messageId: UUID) {
        this.messageId = messageId
    }

    /**
     * Get user's mnemonic as word array for blockchain operations
     * @throws IllegalStateException if user has no wallet
     */
    private fun getUserMnemonicWords(): List<String> {
        val wallet = walletService.getUserWallet(userId)
            ?: throw IllegalStateException("User $userId has no wallet. Cannot perform blockchain operations.")

        val mnemonicPhrase = walletService.decryptMnemonic(wallet)
        return mnemonicPhrase.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private data class TonToUsdtDto(
        val symbol: String,
        val price: Float,
    )

    override fun getTonToUSDT(): Double? {
        return getTonToUSDTStatic()
    }

    override fun sendTonToAddress(amount: Double, receiverAddress: String) {
        val mnemonicWords = getUserMnemonicWords()

        val payload = mapOf(
            "type" to "agent-llm.send-ton",
            "occurredAt" to Instant.now().toString(),
            "data" to mapOf(
                "messageId" to messageId.toString(),
                "userId" to userId,
                "tonAmount" to amount,
                "receiverAddress" to receiverAddress,
                "mnemonic" to mnemonicWords
            )
        )

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "agent-llm.send-ton", payload)
    }

    override fun sendTokenToAddress(tokenAmount: Double, jettonMaster: String, receiverAddress: String) {
        // Convert human-readable token amount to smallest units (nanojettons) using known decimals.
        val decimals = assetsCache.getDecimals(jettonMaster) ?: 9 // fallback if unknown
        val factor = BigDecimal.TEN.pow(decimals)
        val nanoAmount = BigDecimal.valueOf(tokenAmount)
            .multiply(factor)
            .setScale(0, RoundingMode.CEILING)
            .toLong()

        val payload = mapOf(
            "type" to "agent-llm.send-token",
            "occurredAt" to Instant.now().toString(),
            "data" to mapOf(
                "messageId" to messageId.toString(),
                "userId" to userId,
                // keep original human amount for reporting
                "tokenAmount" to tokenAmount,
                "tokenAmountNano" to nanoAmount,
                "jettonMaster" to jettonMaster,
                "receiverAddress" to receiverAddress
            )
        )

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "agent-llm.send-token", payload)
    }



    override fun swapTonToToken(jettonMaster: String, minimalTokenAmount: Double) {
        val mnemonicWords = getUserMnemonicWords()

        val (tokenToTonRate, _) = getTokenToTon(jettonMaster)
        val swapTonAmount = tokenToTonRate?.let {
            // minimalTokenAmount tokens * (TON per token) = required TON (mid-price estimate)
            val slippageSafetyFactor = 1.1 // +10% TON on top of mid-price estimate
            (minimalTokenAmount * it * slippageSafetyFactor)
                .toBigDecimal()
                .setScale(6, RoundingMode.HALF_UP)
                .toDouble()
        }

        val bestPool = poolsCache.getBestPoolByTokenAndTon(jettonMaster)
        val poolAddress = bestPool?.address

        val data = mutableMapOf<String, Any?>(
            "messageId" to messageId.toString(),
            "userId" to userId,
            "jettonMaster" to jettonMaster,
            "minimalTokenAmount" to minimalTokenAmount,
            "poolAddress" to poolAddress,
            "mnemonic" to mnemonicWords
        )
        if (swapTonAmount != null) data["swapTonAmount"] = swapTonAmount


        val payload = mapOf(
            "type" to "agent-llm.swap-ton-to-token",
            "occurredAt" to Instant.now().toString(),
            "data" to data
        )

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "agent-llm.swap-ton-to-token", payload)
    }

    override fun swapTokenToTon(jettonMaster: String, minimalTonAmount: Double) {
        val mnemonicWords = getUserMnemonicWords()

        val (tokenToTonRate, _) = getTokenToTon(jettonMaster)

        // Compute how many tokens are needed, then convert to smallest units (nanojettons)
        val swapTokenAmountNano: Long? = tokenToTonRate?.let { rate ->
            // minimalTonAmount TON / (TON per token) = required tokens (mid-price estimate)
            val tokens = BigDecimal.valueOf(minimalTonAmount) // tokens in units (not nano)
                .divide(BigDecimal.valueOf(rate), 12, RoundingMode.HALF_UP)


            val decimals = assetsCache.getDecimals(jettonMaster) ?: 9 // fallback if missing
            val factor = BigDecimal.TEN.pow(decimals)

            tokens.multiply(factor)

                .setScale(0, RoundingMode.CEILING)
                .toLong()
        }


        val bestPool = poolsCache.getBestPoolByTokenAndTon(jettonMaster)
        val poolAddress = bestPool?.address

        val data = mutableMapOf<String, Any?>(
            "messageId" to messageId.toString(),
            "userId" to userId,
            "jettonMaster" to jettonMaster,
            "minimalTonAmount" to minimalTonAmount,
            "poolAddress" to poolAddress,
            "mnemonic" to mnemonicWords
        )
        if (swapTokenAmountNano != null) data["swapTokenAmount"] = swapTokenAmountNano


        val payload = mapOf(
            "type" to "agent-llm.swap-token-to-ton",
            "occurredAt" to Instant.now().toString(),
            "data" to data,
        )

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "agent-llm.swap-token-to-ton", payload)
    }

    override fun getTokenToTon(jettonMaster: String): Pair<Double?, Double?> =
        try {
            val poolAddress = poolsCache.getBestPoolByTokenAndTon(jettonMaster)?.address
                ?: error("No pool for $jettonMaster found")

            logger.debug { "Pool address for $jettonMaster is $poolAddress" }

            val tokenUsdtPrice = assetsCache.getDexUsdPrice(jettonMaster)
            val tonUsdtPrice = getTonToUSDT() ?: return null to null

            logger.debug { "tokenUsdtPrice=$tokenUsdtPrice, tonUsdtPrice=$tonUsdtPrice" }

            // token_to_ton = token_usdt / ton_usdt
            val price = tokenUsdtPrice?.let { price ->
                if (price > 0.0 && tonUsdtPrice > 0.0) {
                    (price / tonUsdtPrice).toBigDecimal().setScale(6, RoundingMode.HALF_UP).toDouble()
                } else null
            }

            logger.debug { "Calculated price $price in TON of $jettonMaster" }

            price to tokenUsdtPrice
        } catch (e: Exception) {
            logger.debug(e) { "Get token $jettonMaster to TON rate failed with exception" }
            null to null
        }

    override fun getCandidateAssets(symbol: String): String {
        val candidates = assetsCache.findCandidates(symbol)
        if (candidates.isEmpty()) return ""

        val best = candidates.maxByOrNull { it.popularityIndex ?: Double.NEGATIVE_INFINITY }
        return best?.toString() ?: ""
    }

    override fun createPriceTracker(jettonMaster: String, targetPrice: Double) {
        priceTrackerService.createTracker(userId, jettonMaster, targetPrice)
    }

    override fun listPriceTrackers(): String {
        val trackers = priceTrackerService.listUntriggeredByUser(userId)
        if (trackers.isEmpty()) return ""

        return trackers.joinToString(separator = "\n") { t ->
            val asset = assetsCache.getAssetByContractAddress(t.jettonMaster)
            val ticker = asset?.symbol
            "[ticker=${ticker}, jettonMaster=${t.jettonMaster}, targetPrice=${t.targetPrice}, createdAt=${t.createdAt}, id=${t.id}]"
        }
    }


    override fun deletePriceTrackers(ids: List<Long>) {
        ids.forEach {
            priceTrackerService.deleteById(it)
        }
    }

    override fun createOrder(jettonMaster: String, action: String, amount: Double, targetPrice: Double) {

        priceTrackerService.createOrderWithTracker(
            userId = userId,
            jettonMaster = jettonMaster,
            action = action,
            amount = amount,
            targetPrice = targetPrice,
        )
    }

    override fun listUnfulfilledOrders(): String {
        val orders = orderService.listUnfulfilledOrdersByUser(userId)
        if (orders.isEmpty()) return ""

        val trackersByOrderId = priceTrackerService.listByUser(userId)
            .filter { it.orderId != null }
            .associateBy { it.orderId }

        return orders.joinToString(separator = "\n") { o ->
            val tracker = trackersByOrderId[o.id]
            val targetPrice = tracker?.targetPrice
            val asset = assetsCache.getAssetByContractAddress(o.jettonMaster)
            val ticker = asset?.symbol
            "[ticker=${ticker}, action=${o.action}, amount=${o.amount}, " +
                "targetPrice=${targetPrice}, createdAt=${o.createdAt}, id=${o.id}]"
        }
    }


    override suspend fun awaitExternalResults(toolResponses: List<ToolResponse>): List<ToolResponse> =
        coroutineScope {
            toolResponses.map { tr ->
                if (tr.name.contains("send", ignoreCase = true) ||
                    tr.name.contains("swap", ignoreCase = true)
                ) {
                    async {
                        val finalReport = withTimeout(60_000L) {
                            externalToolResultService
                                .registerWait(messageId, tr.name)
                                .await()
                        }
                        tr.copy(responseData = finalReport)
                    }

                } else {
                    async { tr }
                }
            }.awaitAll()
        }
}
