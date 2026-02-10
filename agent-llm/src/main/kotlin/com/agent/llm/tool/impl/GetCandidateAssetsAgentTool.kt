package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.dto.GetCandidateAssetsArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class GetCandidateAssetsAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<GetCandidateAssetsArgs>() {
    override val definition = ToolDefinition(
        name = "get_candidate_assets",
        description = "Get best available assets in TON network (tokens) based on symbol parameter",
        argumentsSchema = ExplytJsonSchema(GetCandidateAssetsArgs::class)
    )

    override val argsSerializer = serializer<GetCandidateAssetsArgs>()

    override fun payload(args: GetCandidateAssetsArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        return bcAdapter.getCandidateAssets(args.symbol)
    }
}