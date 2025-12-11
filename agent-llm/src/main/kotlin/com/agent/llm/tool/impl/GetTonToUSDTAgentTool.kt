package com.agent.llm.tool.impl

import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.dto.GetTonToUSDTArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class GetTonToUSDTAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<GetTonToUSDTArgs>() {
    override val definition = ToolDefinition(
        name = "get_ton_to_usdt_exchange_rate",
        description = "Get current TON to USDT exchange rate",
        argumentsSchema = ExplytJsonSchema(GetTonToUSDTArgs::class)
    )

    override val argsSerializer = serializer<GetTonToUSDTArgs>()

    override fun payload(args: GetTonToUSDTArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        return bcAdapter.getTonToUSDT().toString()
    }
}
