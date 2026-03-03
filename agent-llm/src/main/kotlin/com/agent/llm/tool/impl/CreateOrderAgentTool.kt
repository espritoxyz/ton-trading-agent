package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.dto.CreateOrderArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class CreateOrderAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<CreateOrderArgs>() {
    override val definition = ToolDefinition(
        name = "create_order",
        description = "Create an order for token specified by jettonMaster, with action specifying either sell or buy, and amount",
        argumentsSchema = ExplytJsonSchema(CreateOrderArgs::class)
    )

    override val argsSerializer = serializer<CreateOrderArgs>()

    override fun payload(args: CreateOrderArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        val result = bcAdapter.createOrder(
            jettonMaster = args.jettonMaster,
            action = args.action,
            amount = args.amount,
            targetPrice = args.targetPrice,
            receivedJettonMaster = args.receivedJettonMaster,
            direction = args.direction,
        )

        return result
    }
}
