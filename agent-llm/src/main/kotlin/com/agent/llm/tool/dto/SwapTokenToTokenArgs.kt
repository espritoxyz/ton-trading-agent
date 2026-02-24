package com.agent.llm.tool.dto

import com.agent.llm.tool.api.AgentToolArgs
import kotlinx.serialization.Serializable

@Serializable
data class SwapTokenToTokenArgs(
    val offerJettonMaster: String,
    val askJettonMaster: String,
    val askTokenAmount: Double? = null,
    val offerTokenAmount: Double? = null,
) : AgentToolArgs
