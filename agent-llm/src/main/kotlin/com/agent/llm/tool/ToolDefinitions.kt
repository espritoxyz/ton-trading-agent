package com.agent.llm.tool

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.impl.GetTonToUSDTAgentTool
import com.agent.llm.tool.impl.SendTonAgentTool
import com.agent.llm.tool.impl.SwapTonToTokenAgentTool
import com.agent.llm.tool.impl.TokenToTonAgentTool

class ToolDefinitions(
    bcAdapter: BlockchainAdapter
) {
    val allTools: List<AgentTool<*>> = listOf(
        GetTonToUSDTAgentTool(bcAdapter),
        TokenToTonAgentTool(bcAdapter),
        SendTonAgentTool(bcAdapter),
        SwapTonToTokenAgentTool(bcAdapter)
    )
}
