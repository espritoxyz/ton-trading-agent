package com.agent.llm.tool.dto

import com.agent.llm.tool.api.AgentToolArgs
import kotlinx.serialization.Serializable

@Serializable
data class SwapTonToTokenArgs(
    val userId: Long,
    val jettonMaster: String,
    val minimalTokenAmount: Double,
) : AgentToolArgs
