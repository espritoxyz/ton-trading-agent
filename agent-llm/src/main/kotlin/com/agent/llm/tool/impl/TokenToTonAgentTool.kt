package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.dto.TokenToTonArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class TokenToTonAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<TokenToTonArgs>() {
    override val definition = ToolDefinition(
        name = "get_token_to_ton_exchange_rate",
        description = "Get token, specified by jetton master, to TON and to USD exchange rate (price) accordingly",
        argumentsSchema = ExplytJsonSchema(TokenToTonArgs::class)
    )

    override val argsSerializer = serializer<TokenToTonArgs>()

    override suspend fun payload(args: TokenToTonArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        val (tonPrice, usdPrice) = bcAdapter.getTokenToTon(args.jettonMaster)
        return "[tonPrice=$tonPrice, usdPrice=$usdPrice]"
    }
}
