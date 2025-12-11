package com.agent.backend.llm

import com.agent.backend.rabbitmq.RabbitConfig
import com.agent.llm.tool.api.BlockchainAdapter
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class AgentBlockchainAdapter(
    userId: Long,
    private val rabbitTemplate: RabbitTemplate,
    private val messageId: UUID
) : BlockchainAdapter(userId)  {

    private val binanceClient: RestClient = RestClient.builder()
        .baseUrl("https://api.binance.com/api/v3")
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
}
