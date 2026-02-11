package com.agent.llm.tool.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderArgs(
    val jettonMaster: String,
    val action: String,
    val amount: Double,
    val targetPrice: Double,
)
