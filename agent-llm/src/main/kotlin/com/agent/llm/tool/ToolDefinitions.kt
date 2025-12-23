package com.agent.llm.tool

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.impl.GetTonToUSDTAgentTool
import com.agent.llm.tool.impl.SendTonAgentTool

class ToolDefinitions(
    bcAdapter: BlockchainAdapter
) {
    val allTools: List<AgentTool<*>> = listOf(
        GetTonToUSDTAgentTool(bcAdapter),
        SendTonAgentTool(bcAdapter),
    )
}
