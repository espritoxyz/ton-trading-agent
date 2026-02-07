package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.api.ConfirmationRequired
import com.agent.llm.tool.dto.CreatePriceTrackerArgs
import com.agent.llm.tool.dto.ListPriceTrackersArgs
import com.agent.llm.tool.dto.SendTonArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class ListPriceTrackersAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<ListPriceTrackersArgs>() {
    override val definition = ToolDefinition(
        name = "list_price_trackers",
        description = "List existing jetton price trackers for specified user",
        argumentsSchema = ExplytJsonSchema(ListPriceTrackersArgs::class)
    )

    override val argsSerializer = serializer<ListPriceTrackersArgs>()

    override fun payload(args: ListPriceTrackersArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }

        return bcAdapter.listPriceTrackers()
    }
}
