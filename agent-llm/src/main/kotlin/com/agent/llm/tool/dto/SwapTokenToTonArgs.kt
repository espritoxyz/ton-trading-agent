package com.agent.llm.tool.dto

import com.agent.llm.tool.api.AgentToolArgs
import kotlinx.serialization.Serializable

/**
 * Exactly one of [minimalTonAmount] or [offerTokenAmount] must be provided.
 * - [minimalTonAmount] — how much TON the user wants to receive at minimum (backend back-calculates the required token offer).
 * - [offerTokenAmount] — exact number of tokens the user wants to spend (backend converts directly to nanojettons).
 */
@Serializable
data class SwapTokenToTonArgs(
    val jettonMaster: String,
    val jettonTicker: String,
    val minimalTonAmount: Double? = null,
    val offerTokenAmount: Double? = null,
) : AgentToolArgs
