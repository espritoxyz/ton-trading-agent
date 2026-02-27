package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.dto.DeleteOrdersArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class DeleteOrdersAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<DeleteOrdersArgs>() {
    override val definition = ToolDefinition(
        name = "delete_orders",
        description = "Delete trading orders by provided ids",
        argumentsSchema = ExplytJsonSchema(DeleteOrdersArgs::class)
    )

    override val argsSerializer = serializer<DeleteOrdersArgs>()

    override suspend fun payload(args: DeleteOrdersArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        bcAdapter.deleteOrders(args.orderIds)

        return "Orders deleted successfully"
    }
}

