package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.api.ConfirmationRequired
import com.agent.llm.tool.dto.SwapTokenToTonArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class SwapTokenToTonAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<SwapTokenToTonArgs>(), ConfirmationRequired {
    override val definition = ToolDefinition(
        name = "swap_token_to_ton",
        description = "Swap user's token specified by jetton master to TON" +
                "using minimal requested TON amount ${additionalDescriptionText()}",
        argumentsSchema = ExplytJsonSchema(SwapTokenToTonArgs::class)
    )

    override val argsSerializer = serializer<SwapTokenToTonArgs>()

    override fun payload(args: SwapTokenToTonArgs): String = with(args) {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        bcAdapter.swapTokenToTon(jettonMaster, minimalTonAmount)

        return "Swap of $jettonTicker to receive at least $minimalTonAmount TON initiated"
    }

    override fun confirmationText(args: String): String {
        val serArgs = Json.decodeFromString(argsSerializer, args)
        return with(serArgs) {
            "Swap token $jettonTicker to receive at least $minimalTonAmount TON"
        }
    }
}
