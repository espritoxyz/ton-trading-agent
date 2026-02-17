package com.agent.llm.tool

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.impl.CreateOrderAgentTool
import com.agent.llm.tool.impl.CreatePriceTrackerAgentTool
import com.agent.llm.tool.impl.DeleteOrdersAgentTool
import com.agent.llm.tool.impl.DeletePriceTrackersAgentTool
import com.agent.llm.tool.impl.GetCandidateAssetsAgentTool

import com.agent.llm.tool.impl.GetTonToUSDTAgentTool
import com.agent.llm.tool.impl.ListOrdersAgentTool
import com.agent.llm.tool.impl.ListPriceTrackersAgentTool
import com.agent.llm.tool.impl.SendTokenAgentTool
import com.agent.llm.tool.impl.SendTonAgentTool
import com.agent.llm.tool.impl.ShowTopUpAgentTool
import com.agent.llm.tool.impl.SwapTokenToTonAgentTool
import com.agent.llm.tool.impl.SwapTonToTokenAgentTool
import com.agent.llm.tool.impl.TokenToTonAgentTool

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
        CreateOrderAgentTool(bcAdapter),
        DeleteOrdersAgentTool(bcAdapter),

        ListOrdersAgentTool(bcAdapter),
        ShowTopUpAgentTool(bcAdapter),
    )
}
