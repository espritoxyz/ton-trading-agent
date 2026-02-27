package com.agent.backend.llm

import com.agent.backend.AppUtils
import com.agent.backend.rabbitmq.RabbitConfig

import com.agent.backend.service.ExternalToolResultService
import com.agent.backend.service.NotificationService
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
    private val notificationService: NotificationService,
    private val appUtils: AppUtils,
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

    override fun getTonToUSDT(): Double? {
        return getTonToUSDTStatic()
    }

    override fun sendTonToAddress(amount: Double, receiverAddress: String) {
        val wallet = walletService.getUserWallet(userId)
            ?: throw IllegalStateException("User $userId has no wallet")
        val mnemonicWords = getUserMnemonicWords()

        val payload = mapOf(
            "type" to "agent-llm.send-ton",
            "occurredAt" to Instant.now().toString(),
            "data" to mapOf(
                "messageId" to messageId.toString(),
                "userId" to userId,
                "walletAddress" to wallet.walletAddress,
                "tonAmount" to amount,
                "receiverAddress" to receiverAddress,
                "mnemonic" to mnemonicWords
            )
        )

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "agent-llm.send-ton", payload)
    }

    override fun sendTokenToAddress(tokenAmount: Double, jettonMaster: String, receiverAddress: String) {
        val wallet = walletService.getUserWallet(userId)
            ?: throw IllegalStateException("User $userId has no wallet")
        val asset = assetsCache.getAssetByContractAddress(jettonMaster)
        // Convert human-readable token amount to smallest units (nanojettons) using known decimals.
        val decimals = asset?.decimals ?: 9 // fallback if unknown
        val factor = BigDecimal.TEN.pow(decimals)
        val nanoAmount = BigDecimal.valueOf(tokenAmount)
            .multiply(factor)
            .setScale(0, RoundingMode.CEILING)
            .toLong()

        val mnemonicWords = getUserMnemonicWords()

        val payload = mapOf(
            "type" to "agent-llm.send-token",
            "occurredAt" to Instant.now().toString(),
            "data" to mapOf(
                "messageId" to messageId.toString(),
                "userId" to userId,
                "walletAddress" to wallet.walletAddress,
                // keep original human amount for reporting
                "tokenAmount" to tokenAmount,
                "tokenAmountNano" to nanoAmount,
                "jettonMaster" to jettonMaster,
                "receiverAddress" to receiverAddress,
                "mnemonic" to mnemonicWords
            )
        )

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "agent-llm.send-token", payload)
    }


    override fun swapTonToToken(jettonMaster: String, minimalTokenAmount: Double) {
        val wallet = walletService.getUserWallet(userId)
            ?: throw IllegalStateException("User $userId has no wallet")
        val mnemonicWords = getUserMnemonicWords()

        val bestPool = try {
            poolsCache.getBestPoolByTokenAndTon(jettonMaster)
        } catch (e: StonfiPoolsCacheService.NoSupportedPoolException) {
            val msg = "Swap TON->token rejected: unsupported pool for $jettonMaster"
            externalToolResultService.complete(
                messageId = messageId,
                toolName = "swap_ton_to_token",
                result = msg,
            )
            logger.warn(e) { msg }
            return
        } catch (e: StonfiPoolsCacheService.LowTvlPoolException) {
            val msg = "Swap TON->token rejected: low TVL for $jettonMaster"
            externalToolResultService.complete(
                messageId = messageId,
                toolName = "swap_ton_to_token",
                result = msg,
            )
            logger.warn(e) { "Swap TON->token rejected: low TVL for $jettonMaster" }
            return
        }
        val poolAddress = bestPool?.address

        // We still need numeric rate internally for swap calculation; reuse price logic here.
        val (tokenToTonRate, _) = computeTokenToTonInternal(jettonMaster)
        val swapTonAmount = tokenToTonRate?.let {
            // minimalTokenAmount tokens * (TON per token) = required TON (mid-price estimate)
            (minimalTokenAmount * it)
                .toBigDecimal()
                .setScale(6, RoundingMode.HALF_UP)
                .toDouble()
        }


        val data = mutableMapOf(
            "messageId" to messageId.toString(),
            "userId" to userId,
            "walletAddress" to wallet.walletAddress,
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

    override fun swapTokenToToken(
        offerJettonMaster: String,
        askJettonMaster: String,
        askTokenAmount: Double?,
        offerTokenAmount: Double?,
    ): String {
        if (!appUtils.isStablecoin(offerJettonMaster) && !appUtils.isStablecoin(askJettonMaster)) {
            val msg = "Swap token->token rejected: non-TON/non-USDT pools not supported"
            externalToolResultService.complete(
                messageId = messageId,
                toolName = "swap_token_to_token",
                result = msg,
            )
            logger.warn { msg }
            return msg
        }

        val effectiveAsk = askTokenAmount ?: 0.0
        val effectiveOffer = offerTokenAmount ?: 0.0

        if (effectiveAsk == 0.0 && effectiveOffer == 0.0) {
            val msg = "Swap token->token rejected: unknown swap amount"
            externalToolResultService.complete(
                messageId = messageId,
                toolName = "swap_token_to_token",
                result = msg,
            )
            logger.warn { msg }
            return msg
        }

        return try {
            val wallet = walletService.getUserWallet(userId)
                ?: throw IllegalStateException("User $userId has no wallet")
            val mnemonicWords = getUserMnemonicWords()

            val offerTokensHuman: BigDecimal = if (effectiveAsk > 0.0) {
                // We have desired ask amount -> compute required offer amount via TON prices.
                val (offerTokenToTonRate, _) = computeTokenToTonInternal(offerJettonMaster)
                val (askTokenToTonRate, _) = computeTokenToTonInternal(askJettonMaster)

                if (offerTokenToTonRate == null || askTokenToTonRate == null) {
                    val msg = "Swap token->token rejected: cannot get TON prices for $offerJettonMaster or $askJettonMaster"
                    externalToolResultService.complete(
                        messageId = messageId,
                        toolName = "swap_token_to_token",
                        result = msg,
                    )
                    logger.warn { msg }
                    return msg
                }

                BigDecimal.valueOf(effectiveAsk)
                    .multiply(BigDecimal.valueOf(askTokenToTonRate))
                    .divide(BigDecimal.valueOf(offerTokenToTonRate), 12, RoundingMode.HALF_UP)
            } else {
                // We have offer amount directly -> just use it.
                BigDecimal.valueOf(effectiveOffer)
            }

            val offerAsset = assetsCache.getAssetByContractAddress(offerJettonMaster)
            val offerDecimals = offerAsset?.decimals ?: 9
            val offerFactor = BigDecimal.TEN.pow(offerDecimals)
            val swapOfferTokenAmountNano = offerTokensHuman
                .multiply(offerFactor)
                .setScale(0, RoundingMode.CEILING)
                .toLong()

            val data = mutableMapOf(
                "messageId" to messageId.toString(),
                "userId" to userId,
                "walletAddress" to wallet.walletAddress,
                "offerJettonMaster" to offerJettonMaster,
                "askJettonMaster" to askJettonMaster,
                "askTokenAmount" to if (effectiveAsk > 0.0) effectiveAsk else null,
                "poolAddress" to null,
                "mnemonic" to mnemonicWords,
            )
            data["swapOfferTokenAmount"] = swapOfferTokenAmountNano

            val payload = mapOf(
                "type" to "agent-llm.swap-token-to-token",
                "occurredAt" to Instant.now().toString(),
                "data" to data,
            )

            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "agent-llm.swap-token-to-token", payload)

            if (effectiveAsk > 0.0) {
                "Token-to-token swap from $offerJettonMaster to $askJettonMaster initiated to receive at least $effectiveAsk target units"
            } else {
                "Token-to-token swap from $offerJettonMaster to $askJettonMaster initiated offering $effectiveOffer units"
            }
        } catch (e: Exception) {
            val msg = "Failed to initiate token-to-token swap: ${e.message}"
            externalToolResultService.complete(
                messageId = messageId,
                toolName = "swap_token_to_token",
                result = msg,
            )
            logger.warn(e) { msg }
            msg
        }
    }


    override fun swapTokenToTon(jettonMaster: String, minimalTonAmount: Double) {
        val wallet = walletService.getUserWallet(userId)
            ?: throw IllegalStateException("User $userId has no wallet")
        val mnemonicWords = getUserMnemonicWords()

        val bestPool = try {
            poolsCache.getBestPoolByTokenAndTon(jettonMaster)
        } catch (e: StonfiPoolsCacheService.NoSupportedPoolException) {
            val msg = "Swap token->TON rejected: unsupported pool for $jettonMaster"
            externalToolResultService.complete(
                messageId = messageId,
                toolName = "swap_token_to_ton",
                result = msg,
            )
            logger.warn(e) { msg }
            return
        } catch (e: StonfiPoolsCacheService.LowTvlPoolException) {
            val msg = "Swap token->TON rejected: low TVL for $jettonMaster"
            externalToolResultService.complete(
                messageId = messageId,
                toolName = "swap_token_to_ton",
                result = msg,
            )
            logger.warn(e) { msg }
            return
        }

        val poolAddress = bestPool?.address
        val (tokenToTonRate, _) = computeTokenToTonInternal(jettonMaster)

        // Compute how many tokens are needed, then convert to smallest units (nanojettons)
        val swapTokenAmountNano: Long? = tokenToTonRate?.let { rate ->
            // minimalTonAmount TON / (TON per token) = required tokens (mid-price estimate)
            val tokens = BigDecimal.valueOf(minimalTonAmount) // tokens in units (not nano)
                .divide(BigDecimal.valueOf(rate), 12, RoundingMode.HALF_UP)

            val asset = assetsCache.getAssetByContractAddress(jettonMaster)
            val decimals = asset?.decimals ?: 9 // fallback if missing
            val factor = BigDecimal.TEN.pow(decimals)

            tokens.multiply(factor)
                .setScale(0, RoundingMode.CEILING)
                .toLong()
        }


        val data = mutableMapOf(
            "messageId" to messageId.toString(),
            "userId" to userId,
            "walletAddress" to wallet.walletAddress,
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

    // Internal numeric price calculator reused by swap methods.
    private fun computeTokenToTonInternal(jettonMaster: String): Pair<Double?, Double?> {
        val poolAddress = try {
            poolsCache.getBestPoolByTokenAndTon(jettonMaster)?.address
                ?: error("No pool for $jettonMaster found")
        } catch (e: StonfiPoolsCacheService.NoSupportedPoolException) {
            logger.warn(e) { "Internal price lookup rejected: unsupported pool for $jettonMaster — ${e.message}" }
            return null to null
        } catch (e: StonfiPoolsCacheService.LowTvlPoolException) {
            logger.warn(e) { "Internal price lookup rejected: low TVL for $jettonMaster — ${e.message}" }
            return null to null
        }

        logger.debug { "Pool address for $jettonMaster is $poolAddress" }
        val asset = assetsCache.getAssetByContractAddress(jettonMaster)
        val tokenUsdtPrice = asset?.dexUsdPrice
        val tonUsdtPrice = getTonToUSDT() ?: return null to null

        logger.debug { "tokenUsdtPrice=$tokenUsdtPrice, tonUsdtPrice=$tonUsdtPrice" }

        val price = tokenUsdtPrice?.let { price ->
            if (price > 0.0 && tonUsdtPrice > 0.0) {
                (price / tonUsdtPrice).toBigDecimal().setScale(6, RoundingMode.HALF_UP).toDouble()
            } else null
        }

        logger.debug { "Calculated price $price in TON of $jettonMaster" }

        return price to tokenUsdtPrice
    }

    override fun getTokenToTon(jettonMaster: String): String {
        return try {
            val (tonPrice, usdPrice) = computeTokenToTonInternal(jettonMaster)
            "[tonPrice=$tonPrice, usdPrice=$usdPrice]"
        } catch (e: Exception) {
            val msg = "Failed to get token->TON price for $jettonMaster: ${e.message}"
            logger.warn(e) { msg }
            msg
        }
    }

    override fun getCandidateAssets(symbol: String): String {
        val candidates = assetsCache.findCandidates(symbol)
        if (candidates.isEmpty()) {
            logger.warn { "Candidates list for $symbol is empty" }
            return ""
        }

        val best = candidates.maxByOrNull { it.popularityIndex ?: Double.NEGATIVE_INFINITY }
        logger.debug { "Best candidate asser for $symbol is $best" }
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

    override fun deleteOrders(ids: List<Long>) {
        ids.forEach {
            orderService.deleteByIdForUser(userId, it)
        }
    }

    override fun createOrder(
        jettonMaster: String,
        action: String,
        amount: Double,
        targetPrice: Double,
        receivedJettonMaster: String?,
    ): String {
        return try {
            // If LLM/tool didn't specify, default received asset to TON from address book
            val effectiveReceived = receivedJettonMaster ?: appUtils.tonAddress

            priceTrackerService.createOrderWithTracker(
                userId = userId,
                jettonMaster = jettonMaster,
                action = action,
                amount = amount,
                targetPrice = targetPrice,
                receivedJettonMaster = effectiveReceived,
            )
            notificationService.broadcastWalletRefresh(userId)

            "Order created for $jettonMaster: action=$action, amount=$amount, targetPrice=$targetPrice, receive in $effectiveReceived"
        } catch (e: Exception) {
            val msg = "Failed to create order for $jettonMaster: ${e.message}"
            logger.warn(e) { msg }
            msg
        }
    }


    override fun listOrders(activeOnly: Boolean): String {
        val orders = if (activeOnly)
            orderService.listUnfulfilledOrdersByUser(userId)
        else
            orderService.listAllOrdersByUser(userId)

        if (orders.isEmpty()) return ""

        val trackersByOrderId = priceTrackerService.listByUser(userId)
            .filter { it.orderId != null }
            .associateBy { it.orderId }

        return orders.joinToString(separator = "\n") { o ->
            val tracker = trackersByOrderId[o.id]
            val targetPrice = tracker?.targetPrice
            val asset = assetsCache.getAssetByContractAddress(o.jettonMaster)
            val ticker = asset?.symbol
            "[ticker=${ticker}, action=${o.action}, amount=${o.amount}, isActive=${!o.fulfilled}," +
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
