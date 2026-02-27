package com.agent.llm.tool.api

import com.explyt.ai.dto.ToolResponse
import java.util.*

abstract class BlockchainAdapter(val userId: Long) {
    abstract fun updateCurrentMessageId(messageId: UUID)

    abstract fun getTonToUSDT(): Double?

    /**
     * Returns textual description of token->TON and token->USD prices, or an error message.
     */
    abstract fun getTokenToTon(jettonMaster: String): String

    abstract fun sendTonToAddress(amount: Double, receiverAddress: String)


    abstract fun sendTokenToAddress(tokenAmount: Double, jettonMaster: String, receiverAddress: String)

    abstract fun swapTonToToken(jettonMaster: String, minimalTokenAmount: Double)

    abstract fun swapTokenToTon(jettonMaster: String, minimalTonAmount: Double)

    abstract fun swapTokenToToken(
        offerJettonMaster: String,
        askJettonMaster: String,
        askTokenAmount: Double? = null,
        offerTokenAmount: Double? = null,
    ): String


    abstract fun getCandidateAssets(symbol: String): String

    abstract fun createPriceTracker(jettonMaster: String, targetPrice: Double)

    abstract fun listPriceTrackers(): String

    abstract fun deletePriceTrackers(ids: List<Long>)

    abstract fun createOrder(
        jettonMaster: String,
        action: String,
        amount: Double,
        targetPrice: Double,
        receivedJettonMaster: String? = null,
    ): String


    abstract fun listOrders(activeOnly: Boolean): String

    abstract fun deleteOrders(ids: List<Long>)


    open suspend fun awaitExternalResults(toolResponses: List<ToolResponse>): List<ToolResponse> = toolResponses
}








