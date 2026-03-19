package com.agent.llm.tool.dto

import com.agent.llm.tool.api.AgentToolArgs
import kotlinx.serialization.Serializable

/**
 * Exactly one of [minimalTokenAmount] or [offerTonAmount] must be provided.
 * - [minimalTokenAmount] — how many tokens the user wants to receive at minimum (backend back-calculates the required TON offer).
 * - [offerTonAmount] — exact amount of TON the user wants to spend (backend uses it directly as swapTonAmount).
 */
@Serializable
data class SwapTonToTokenArgs(
    val jettonMaster: String,
    val jettonTicker: String,
    val minimalTokenAmount: Double? = null,
    val offerTonAmount: Double? = null,
) : AgentToolArgs
