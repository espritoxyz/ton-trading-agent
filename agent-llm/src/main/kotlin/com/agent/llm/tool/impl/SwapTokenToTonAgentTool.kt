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
        description = "Swap user's token to TON via Ston.fi. " +
                "Provide either minimalTonAmount (how much TON to receive at minimum — the required token spend is calculated automatically) " +
                "OR offerTokenAmount (exact number of tokens to spend — TON received depends on the current rate). " +
                "Exactly one of the two must be non-null. ${additionalDescriptionText()}",
        argumentsSchema = ExplytJsonSchema(SwapTokenToTonArgs::class)
    )

    override val argsSerializer = serializer<SwapTokenToTonArgs>()

    override suspend fun payload(args: SwapTokenToTonArgs): String = with(args) {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        require(minimalTonAmount != null || offerTokenAmount != null) {
            "swap_token_to_ton: either minimalTonAmount or offerTokenAmount must be provided"
        }
        bcAdapter.swapTokenToTon(jettonMaster, minimalTonAmount, offerTokenAmount)
        return buildResultMessage(args)
    }

    override fun confirmationText(args: String): String {
        val a = Json.decodeFromString(argsSerializer, args)
        return buildResultMessage(a)
    }

    private fun buildResultMessage(a: SwapTokenToTonArgs): String = when {
        a.offerTokenAmount != null ->
            "Swap ${a.offerTokenAmount} ${a.jettonTicker} → TON"
        else ->
            "Swap ${a.jettonTicker} → TON, receive at least ${a.minimalTonAmount} TON"
    }
}
