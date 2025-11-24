package com.agent.backend.llm

import com.agent.llm.BlockchainAdapter
import java.math.BigDecimal
import java.math.RoundingMode
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

class AgentBlockchainAdapter(userId: Long) : BlockchainAdapter(userId)  {
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

    override fun requestSendTonConfirmation(amount: Double, receiverAddress: String): Boolean {
        TODO("Not yet implemented")
    }


}
