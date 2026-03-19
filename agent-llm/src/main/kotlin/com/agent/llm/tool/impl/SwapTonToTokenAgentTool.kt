package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.api.ConfirmationRequired
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
        description = "Swap user's TON to a token via Ston.fi. " +
                "Provide either minimalTokenAmount (how many tokens to receive at minimum — the required TON spend is calculated automatically) " +
                "OR offerTonAmount (exact amount of TON to spend — tokens received depend on the current rate). " +
                "Exactly one of the two must be non-null. ${additionalDescriptionText()}",
        argumentsSchema = ExplytJsonSchema(SwapTonToTokenArgs::class)
    )

    override val argsSerializer = serializer<SwapTonToTokenArgs>()

    override suspend fun payload(args: SwapTonToTokenArgs): String = with(args) {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        require(minimalTokenAmount != null || offerTonAmount != null) {
            "swap_ton_to_token: either minimalTokenAmount or offerTonAmount must be provided"
        }
        bcAdapter.swapTonToToken(jettonMaster, minimalTokenAmount, offerTonAmount)
        return buildResultMessage(args)
    }

    override fun confirmationText(args: String): String {
        val a = Json.decodeFromString(argsSerializer, args)
        return buildResultMessage(a)
    }

    private fun buildResultMessage(a: SwapTonToTokenArgs): String = when {
        a.offerTonAmount != null ->
            "Swap ${a.offerTonAmount} TON → ${a.jettonTicker}"
        else ->
            "Swap TON → ${a.jettonTicker}, receive at least ${a.minimalTokenAmount} ${a.jettonTicker}"
    }
}
