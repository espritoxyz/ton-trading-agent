package com.agent.llm.tool.dto

import com.agent.llm.tool.api.AgentToolArgs
import kotlinx.serialization.Serializable

@Serializable
data class SendTokenArgs(
    val userId: Long,
    val tokenAmount: Double,
    val jettonMaster: String,
    val receiverAddress: String,
) : AgentToolArgs
