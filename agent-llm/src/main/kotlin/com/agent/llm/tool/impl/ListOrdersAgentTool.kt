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
        description = "List unfulfilled swap orders for the current user.",
        argumentsSchema = ExplytJsonSchema(ListOrdersArgs::class)
    )

    override val argsSerializer = serializer<ListOrdersArgs>()

    override fun payload(args: ListOrdersArgs): String {
        return bcAdapter.listUnfulfilledOrders()
    }
}
