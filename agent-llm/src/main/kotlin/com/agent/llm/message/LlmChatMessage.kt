package com.agent.llm.message

import kotlinx.serialization.Serializable

@Serializable
data class LlmChatMessage(
    val content: String,
    val type: LlmChatMessageType
)
