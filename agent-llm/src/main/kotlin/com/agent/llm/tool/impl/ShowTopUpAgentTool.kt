package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.api.ConfirmationRequired
import com.agent.llm.tool.dto.ShowTopUpArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class ShowTopUpAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<ShowTopUpArgs>(), ConfirmationRequired {
    override val definition = ToolDefinition(
        name = "show_top_up_dialog",
        description = "Show top-up dialog to the user when they request to deposit or add funds to their wallet. This will display deposit instructions with a unique deposit code and wallet address.",
        argumentsSchema = ExplytJsonSchema(ShowTopUpArgs::class)
    )

    override val argsSerializer = serializer<ShowTopUpArgs>()

    override suspend fun payload(args: ShowTopUpArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }

        return "Top-up dialog has been displayed. The user can click the button to view deposit instructions with a unique code and wallet address. They need to include the code in the transaction comment when sending TON or Jettons."
    }

    override fun confirmationText(args: String): String {
        return "Click the button below to open the top-up dialog and get your deposit address"
    }
}
