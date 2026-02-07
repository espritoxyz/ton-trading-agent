package com.agent.llm.tool.api

import com.explyt.ai.dto.ToolResponse
import java.util.UUID

abstract class BlockchainAdapter(val userId: Long) {
    abstract fun updateCurrentMessageId(messageId: UUID)

    abstract fun getTonToUSDT(): Double?

    abstract fun getTokenToTon(jettonMaster: String): Double?

    abstract fun sendTonToAddress(amount: Double, receiverAddress: String)

    abstract fun swapTonToToken(jettonMaster: String, minimalTokenAmount: Double)

    abstract fun swapTokenToTon(jettonMaster: String, minimalTonAmount: Double)

    abstract fun getCandidateAssets(symbol: String): String

    abstract fun createPriceTracker(jettonMaster: String, targetPrice: Double)

    abstract fun listPriceTrackers(): String

    open suspend fun awaitExternalResults(toolResponses: List<ToolResponse>): List<ToolResponse> = toolResponses
}


