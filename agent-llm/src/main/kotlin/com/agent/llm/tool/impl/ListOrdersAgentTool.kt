package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.dto.ListOrdersArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import kotlinx.serialization.serializer

class ListOrdersAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<ListOrdersArgs>() {

    override val definition = ToolDefinition(
        name = "list_orders",
        description = "List swap orders for the current user, aggregating them based on user requesting either only active" +
                "orders or all orders",
        argumentsSchema = ExplytJsonSchema(ListOrdersArgs::class)
    )

    override val argsSerializer = serializer<ListOrdersArgs>()

    override suspend fun payload(args: ListOrdersArgs): String {
        return bcAdapter.listOrders(args.showOnlyActiveOrders)
    }
}
