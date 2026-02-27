package com.agent.llm.tool.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderArgs(
    val jettonMaster: String,
    val action: String,
    val amount: Double,
    val targetPrice: Double,
    /**
     * Jetton master of what we want to receive when the order executes.
     * If omitted, the backend will default this to TON's jetton master.
     */
    val receivedJettonMaster: String? = null,
)
