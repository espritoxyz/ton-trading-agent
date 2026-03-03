package com.agent.llm.tool.dto

import com.agent.llm.tool.api.AgentToolArgs
import com.explyt.ai.schema.Description
import kotlinx.serialization.Serializable

@Serializable
data class CreatePriceTrackerArgs(
    val jettonMaster: String,
    val targetPrice: Double,
    @Description("Direction of comparison of current price with the target price. " +
            "If user implies action must be triggered when current price is 'less than' target price, set direction DOWN." +
            "The same applies for UP. Direction EQUAL is a default value when user desires specific price to be reached, without directions.")
    val direction: PriceDirection = PriceDirection.EQUAL,
) : AgentToolArgs
