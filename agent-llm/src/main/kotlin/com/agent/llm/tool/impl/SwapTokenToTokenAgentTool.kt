package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.api.ConfirmationRequired
import com.agent.llm.tool.dto.SwapTokenToTokenArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class SwapTokenToTokenAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<SwapTokenToTokenArgs>(), ConfirmationRequired {
    override val definition = ToolDefinition(
        name = "swap_token_to_token",
        description = "Swap user's one token to another token using either asked or offered amount based on request context",
        argumentsSchema = ExplytJsonSchema(SwapTokenToTokenArgs::class)
    )

    override val argsSerializer = serializer<SwapTokenToTokenArgs>()

    override fun payload(args: SwapTokenToTokenArgs): String = with(args) {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }

        return bcAdapter.swapTokenToToken(
            offerJettonMaster = offerJettonMaster,
            askJettonMaster = askJettonMaster,
            askTokenAmount = askTokenAmount,
            offerTokenAmount = offerTokenAmount,
        )
    }

    override fun confirmationText(args: String): String {
        val serArgs = Json.decodeFromString(argsSerializer, args)
        return with(serArgs) {
            when {
                askTokenAmount != null ->
                    "Swap token $offerJettonTicker to receive at least $askTokenAmount $askJettonTicker"

                offerTokenAmount != null ->
                    "Swap token $offerJettonTicker offering $offerTokenAmount $askJettonTicker"

                else ->
                    "Swap token $offerJettonTicker to $askJettonTicker (no exact amounts specified)"
            }
        }
    }
}


