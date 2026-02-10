package com.agent.llm.tool

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.impl.*

class ToolDefinitions(
    bcAdapter: BlockchainAdapter
) {
    val allTools: List<AgentTool<*>> = listOf(
        GetTonToUSDTAgentTool(bcAdapter),
        TokenToTonAgentTool(bcAdapter),
        SendTonAgentTool(bcAdapter),
        SendTokenAgentTool(bcAdapter),
        SwapTonToTokenAgentTool(bcAdapter),
        SwapTokenToTonAgentTool(bcAdapter),
        GetCandidateAssetsAgentTool(bcAdapter),
        CreatePriceTrackerAgentTool(bcAdapter),
        ListPriceTrackersAgentTool(bcAdapter),
        DeletePriceTrackersAgentTool(bcAdapter),
    )
}
