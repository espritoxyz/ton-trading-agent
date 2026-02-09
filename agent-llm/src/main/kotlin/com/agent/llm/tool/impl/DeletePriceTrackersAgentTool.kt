package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.dto.CreatePriceTrackerArgs
import com.agent.llm.tool.dto.DeletePriceTrackersArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class DeletePriceTrackersAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<DeletePriceTrackersArgs>() {
    override val definition = ToolDefinition(
        name = "delete_price_trackers",
        description = "Delete price trackers by provided ids",
        argumentsSchema = ExplytJsonSchema(DeletePriceTrackersArgs::class)
    )

    override val argsSerializer = serializer<DeletePriceTrackersArgs>()

    override fun payload(args: DeletePriceTrackersArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        bcAdapter.deletePriceTrackers(args.trackerIds)

        return "Tracks for ids deleted: ${args.trackerIds.joinToString(",")}"
    }
}