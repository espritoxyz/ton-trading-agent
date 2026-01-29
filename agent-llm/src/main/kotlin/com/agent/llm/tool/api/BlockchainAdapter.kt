package com.agent.llm.tool.api

import java.util.UUID

abstract class BlockchainAdapter(val userId: Long) {
    abstract fun updateCurrentMessageId(messageId: UUID)

    abstract fun getTonToUSDT(): Double?

    abstract fun getTokenToTon(jettonMaster: String): Double?

    abstract fun sendTonToAddress(amount: Double, receiverAddress: String)

    abstract fun swapTonToToken(jettonMaster: String, minimalTokenAmount: Double)

    abstract fun swapTokenToTon(jettonMaster: String, minimalTonAmount: Double)

    abstract fun getCandidateAssets(symbol: String): String
}
