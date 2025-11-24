package com.agent.llm

abstract class BlockchainAdapter(val userId: Long) {
    abstract fun getTonToUSDT(): Double?

    abstract fun requestSendTonConfirmation(amount: Double, receiverAddress: String): Boolean
}
