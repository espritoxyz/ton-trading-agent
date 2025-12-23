package com.agent.llm.tool.dto

import com.agent.llm.tool.api.AgentToolArgs
import kotlinx.serialization.Serializable

@Serializable
data class SendTonArgs(
    val userId: Long,
    val tonAmount: Double,
    val receiverAddress: String,
) : AgentToolArgs
