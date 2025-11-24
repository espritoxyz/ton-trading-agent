package com.agent.llm.tool.impl

import com.agent.llm.tool.AgentToolArgs
import kotlinx.serialization.Serializable

@Serializable
data class PrepareSendTonArgs(
    val userId: Long,
    val tonAmount: Double,
    val receiverAddress: String,
) : AgentToolArgs