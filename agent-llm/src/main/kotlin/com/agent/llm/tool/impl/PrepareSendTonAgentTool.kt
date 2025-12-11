package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.api.ConfirmationRequired
import com.agent.llm.tool.dto.PrepareSendTonArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class PrepareSendTonAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<PrepareSendTonArgs>(), ConfirmationRequired {
    override val definition = ToolDefinition(
        name = "prepare_send_ton_to_address",
        description = "Prepare sending specified TON amount to given address by creating utility confirmation message",
        argumentsSchema = ExplytJsonSchema(PrepareSendTonArgs::class)
    )

    override val argsSerializer = serializer<PrepareSendTonArgs>()

    override fun payload(args: PrepareSendTonArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        bcAdapter.sendTonToAddress(args.tonAmount, args.receiverAddress)

        return ""
    }

    override fun confirmationText(args: String): String {
        val serArgs = Json.decodeFromString(argsSerializer, args)
        return with(serArgs) {
            "Send $tonAmount TON to $receiverAddress"
        }
    }
}
