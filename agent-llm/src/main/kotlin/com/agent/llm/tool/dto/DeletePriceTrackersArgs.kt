package com.agent.llm.tool.dto

import com.agent.llm.tool.api.AgentToolArgs
import kotlinx.serialization.Serializable

@Serializable
data class DeletePriceTrackersArgs(
    val trackerIds: List<Long>,
) : AgentToolArgs
