package com.agent.llm.tool.impl

import com.agent.llm.BlockchainAdapter
import com.agent.llm.tool.AgentTool
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class PrepareSendTonAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<PrepareSendTonArgs>() {
    override val definition = ToolDefinition(
        name = "prepare_send_ton_to_address",
        description = "Prepare sending specified TON amount to given address by asking for action confirmation",
        argumentsSchema = ExplytJsonSchema(PrepareSendTonArgs::class)
    )

    override val argsSerializer = serializer<PrepareSendTonArgs>()

    override fun payload(args: PrepareSendTonArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        bcAdapter.requestSendTonConfirmation(args.tonAmount, args.receiverAddress)

        return ""
    }
}
