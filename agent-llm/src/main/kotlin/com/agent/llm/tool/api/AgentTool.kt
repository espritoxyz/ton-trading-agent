package com.agent.llm.tool.api

import com.explyt.ai.dto.ToolCall
import com.explyt.ai.dto.ToolDefinition
import kotlinx.serialization.KSerializer

abstract class AgentTool<ToolArgs> {

    abstract val definition: ToolDefinition

    abstract val argsSerializer: KSerializer<ToolArgs>

    abstract fun payload(args: ToolArgs): String

    companion object {
        fun fromToolCall(allTools: List<AgentTool<*>>, toolCall: ToolCall): AgentTool<*>? {
            return allTools.find { tool -> tool.definition.name == toolCall.name }
        }
    }
}

interface ConfirmationRequired {
    fun confirmationText(args: String): String

    fun additionalDescriptionText() = "WITHOUT USER CONFIRMATION IF ALL ARGS ARE GIVEN"
}
