package com.agent.backend.llm

import com.agent.backend.rabbitmq.RabbitConfig
import com.agent.backend.service.StonfiPoolsCacheService
import com.agent.llm.tool.api.BlockchainAdapter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class AgentBlockchainAdapter(
    userId: Long,
    private val rabbitTemplate: RabbitTemplate,
    private val messageId: UUID,
    private val poolsCache: StonfiPoolsCacheService,
) : BlockchainAdapter(userId)  {


    private val COINMARKETCAP_API_TOKEN = "e004ca7d-3fc3-4e57-8441-424806178ff5"
    private val COINMARKETCAP_TON_NETWORK_ID = 173

    private val binanceClient: RestClient = RestClient.builder()
        .baseUrl("https://api.binance.com/api/v3")
        .build()

    private val cmcClient: RestClient = RestClient.builder()
        .baseUrl("https://pro-api.coinmarketcap.com")
        .defaultHeader("X-CMC_PRO_API_KEY", COINMARKETCAP_API_TOKEN)
        .build()

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

    override fun swapTonToToken(jettonMaster: String, minimalTokenAmount: Double) {
        val tokenToTonRate = getTokenToTon(jettonMaster)
        val swapTonAmount = tokenToTonRate?.let {
            // minimalTokenAmount tokens * (TON per token) = required TON (mid-price estimate)
            // add a safety margin to cover slippage and fees so we actually meet the minAsk on-chain
            val slippageSafetyFactor = 1.1 // +10% TON on top of mid-price estimate
            (minimalTokenAmount * it * slippageSafetyFactor)
                .toBigDecimal()
                .setScale(6, RoundingMode.HALF_UP)
                .toDouble()
        }

        // Let backend choose the best pool so both JVM and Node use the same pool
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

    override fun getTokenToTon(jettonMaster: String): Double? {
        return try {
            val poolAddress = poolsCache.getBestPoolByTokenAndTon(jettonMaster)?.address
                ?: error("No pool for $jettonMaster found")

            logger.debug { "Pool address for $jettonMaster is $poolAddress" }

            val response: Any? = cmcClient
                .get()
                .uri { builder ->
                    builder
                        .path("/v4/dex/pairs/quotes/latest")
                        .queryParam("contract_address", poolAddress)
                        .queryParam("network_id", COINMARKETCAP_TON_NETWORK_ID)
                        .queryParam("convert", "USDT")
                        .build()
                }
                .retrieve()
                .body()

            logger.debug { "[CMC] Raw response for pool $poolAddress: $response" }

            val root = response as? Map<*, *>
            val dataList = root?.get("data") as? List<*>
            val first = dataList?.firstOrNull() as? Map<*, *>
            val quotes = first?.get("quote") as? List<*>
            val firstQuote = quotes?.firstOrNull() as? Map<*, *>
            
            val tokenUsdtPrice = (firstQuote?.get("price") as? Number)?.toDouble()
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
    }
}
