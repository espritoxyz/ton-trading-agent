package com.agent.llm.tool.api

abstract class BlockchainAdapter(val userId: Long) {
    abstract fun getTonToUSDT(): Double?

    abstract fun sendTonToAddress(amount: Double, receiverAddress: String)
}
