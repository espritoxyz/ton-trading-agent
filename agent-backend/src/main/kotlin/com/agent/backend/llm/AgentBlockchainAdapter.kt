package com.agent.backend.llm

import com.agent.backend.rabbitmq.RabbitConfig
import com.agent.backend.service.ExternalToolResultService
import com.agent.backend.service.PriceTrackerService
import com.agent.backend.service.StonfiAssetsCacheService
import com.agent.backend.service.StonfiPoolsCacheService
import com.agent.llm.tool.api.BlockchainAdapter
import com.explyt.ai.dto.ToolResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private val priceTrackerService: PriceTrackerService,
    private val externalToolResultService: ExternalToolResultService,
) : BlockchainAdapter(userId) {
    private val binanceClient: RestClient = RestClient.builder()
        .baseUrl("https://api.binance.com/api/v3")
        .build()

    override fun updateCurrentMessageId(messageId: UUID) {
        this.messageId = messageId
    }

    private data class TonToUsdtDto(
        val symbol: String,
        val price: Float,
    )

    override fun getTonToUSDT(): Double? {
        return binanceClient
            .get()
            .uri("/ticker/price?symbol=TONUSDT")
            .retrieve()
            // Nasty code, I know, will fix later
            .body<TonToUsdtDto>()?.price?.toBigDecimal()?.setScale(2, RoundingMode.HALF_UP)?.toDouble()
    }

    override fun sendTonToAddress(amount: Double, receiverAddress: String) {
        val payload = mapOf(
            "type" to "agent-llm.send-ton",
            "occurredAt" to Instant.now().toString(),
            "data" to mapOf(
                "messageId" to messageId.toString(),
                "userId" to userId,
                "tonAmount" to amount,
                "receiverAddress" to receiverAddress
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
        val tokenToTonRate = getTokenToTon(jettonMaster)
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
        val tokenToTonRate = getTokenToTon(jettonMaster)

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
        )
        if (swapTokenAmountNano != null) data["swapTokenAmount"] = swapTokenAmountNano


        val payload = mapOf(
            "type" to "agent-llm.swap-token-to-ton",
            "occurredAt" to Instant.now().toString(),
            "data" to data,
        )

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "agent-llm.swap-token-to-ton", payload)
    }

    override fun getTokenToTon(jettonMaster: String): Double? =
        try {
            val poolAddress = poolsCache.getBestPoolByTokenAndTon(jettonMaster)?.address
                ?: error("No pool for $jettonMaster found")

            logger.debug { "Pool address for $jettonMaster is $poolAddress" }

            val tokenUsdtPrice = assetsCache.getDexUsdPrice(jettonMaster)
            val tonUsdtPrice = getTonToUSDT() ?: return null

            logger.debug { "tokenUsdtPrice=$tokenUsdtPrice, tonUsdtPrice=$tonUsdtPrice" }

            // token_to_ton = token_usdt / ton_usdt
            val price = tokenUsdtPrice?.let { price ->
                if (price > 0.0 && tonUsdtPrice > 0.0) {
                    (price / tonUsdtPrice).toBigDecimal().setScale(6, RoundingMode.HALF_UP).toDouble()
                } else null
            }

            logger.debug { "Calculated price $price in TON of $jettonMaster" }

            price
        } catch (e: Exception) {
            logger.debug(e) { "Get token $jettonMaster to TON rate failed with exception" }
            null
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
            "[jettonMaster=${t.jettonMaster}, targetPrice=${t.targetPrice}, createdAt=${t.createdAt}, id=${t.id}]"
        }
    }

    override fun deletePriceTrackers(ids: List<Long>) {
        ids.forEach {
            priceTrackerService.deleteById(it)
        }
    }

    override suspend fun awaitExternalResults(toolResponses: List<ToolResponse>): List<ToolResponse> =
        coroutineScope {
            toolResponses.map { tr ->
                // For send/swap tools we wait for the final async result from RabbitMQ
                if (tr.name.contains("send", ignoreCase = true) ||
                    tr.name.contains("swap", ignoreCase = true)
                ) {
                    async {
                        val finalReport = externalToolResultService
                            .registerWait(messageId, tr.name)
                            .await()
                        tr.copy(responseData = finalReport)
                    }
                } else {
                    async { tr }
                }
            }.awaitAll()
        }
}


