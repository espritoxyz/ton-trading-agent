package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.dto.GetTonToUSDTArgs
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
        description = "Get token, specified by jetton master, to TON exchange rate",
        argumentsSchema = ExplytJsonSchema(TokenToTonArgs::class)
    )

    override val argsSerializer = serializer<TokenToTonArgs>()

    override fun payload(args: TokenToTonArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        return bcAdapter.getTokenToTon(args.jettonMaster).toString()
    }
}
