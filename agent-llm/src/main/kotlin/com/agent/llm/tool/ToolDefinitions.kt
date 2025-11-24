package com.agent.llm.tool

import com.agent.llm.BlockchainAdapter
import com.agent.llm.tool.impl.GetTonToUSDTAgentTool

class ToolDefinitions(
    bcAdapter: BlockchainAdapter
) {
    val allTools: List<AgentTool<*>> = listOf(
        GetTonToUSDTAgentTool(bcAdapter)
    )
}
