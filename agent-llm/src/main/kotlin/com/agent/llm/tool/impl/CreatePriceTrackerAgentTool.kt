package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.dto.CreatePriceTrackerArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class CreatePriceTrackerAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<CreatePriceTrackerArgs>() {
    override val definition = ToolDefinition(
        name = "create_price_tracker",
        description = "Create a price tracker for provided token (jettonMaster) with targetPrice in USD",
        argumentsSchema = ExplytJsonSchema(CreatePriceTrackerArgs::class)
    )

    override val argsSerializer = serializer<CreatePriceTrackerArgs>()

    override suspend fun payload(args: CreatePriceTrackerArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        bcAdapter.createPriceTracker(args.jettonMaster, args.targetPrice, args.direction)

        return "Track for ${args.jettonMaster} reaching ${args.targetPrice} USD created"
    }
}
