package com.agent.llm.tool.dto

import com.agent.llm.tool.api.AgentToolArgs
import kotlinx.serialization.Serializable

@Serializable
class FetchUrlArgs(
    val url: String,
) : AgentToolArgs
