package com.agent.llm.tool.dto

import com.explyt.ai.schema.Description
import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderArgs(
    val jettonMaster: String,
    val action: String,
    val amount: Double,
    val targetPrice: Double,
    @Description("Direction of comparison of current price with the target price. " +
            "If user implies action must be triggered when current price is 'less than' target price, set direction DOWN." +
            "The same applies for UP. Direction EQUAL is a default value when user desires specific price to be reached, without directions.")
    val direction: PriceDirection = PriceDirection.EQUAL,
    /**
     * Jetton master of what we want to receive when the order executes.
     * If omitted, the backend will default this to TON's jetton master.
     */
    val receivedJettonMaster: String? = null,
)
