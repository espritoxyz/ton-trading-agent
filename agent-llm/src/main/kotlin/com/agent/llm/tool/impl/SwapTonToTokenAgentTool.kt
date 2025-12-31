package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.api.ConfirmationRequired
import com.agent.llm.tool.dto.SendTonArgs
import com.agent.llm.tool.dto.SwapTonToTokenArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class SwapTonToTokenAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<SwapTonToTokenArgs>(), ConfirmationRequired {
    override val definition = ToolDefinition(
        name = "swap_ton_to_token",
        description = "Swap user's TON to token, specified by jetton master, " +
                "using minimal requested token amount ${additionalDescriptionText()}",
        argumentsSchema = ExplytJsonSchema(SwapTonToTokenArgs::class)
    )

    override val argsSerializer = serializer<SwapTonToTokenArgs>()

    override fun payload(args: SwapTonToTokenArgs): String = with(args) {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        bcAdapter.swapTonToToken(jettonMaster, minimalTokenAmount)

        return "Swap of TON to minimal of $minimalTokenAmount $jettonMaster initiated"
    }

    override fun confirmationText(args: String): String {
        val serArgs = Json.decodeFromString(argsSerializer, args)
        return with(serArgs) {
            "Swap TON to minimal of $minimalTokenAmount tokens, Jetton Master $jettonMaster"
        }
    }
}
