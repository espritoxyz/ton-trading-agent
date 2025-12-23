package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.api.ConfirmationRequired
import com.agent.llm.tool.dto.SendTonArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class SendTonAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<SendTonArgs>(), ConfirmationRequired {
    override val definition = ToolDefinition(
        name = "send_ton_to_address",
        description = "Send specified TON amount to given address and return action result ${additionalDescriptionText()}",
        argumentsSchema = ExplytJsonSchema(SendTonArgs::class)
    )

    override val argsSerializer = serializer<SendTonArgs>()

    override fun payload(args: SendTonArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        bcAdapter.sendTonToAddress(args.tonAmount, args.receiverAddress)

        return "Transfer to ${args.receiverAddress} of ${args.tonAmount} TON initiated"
    }

    override fun confirmationText(args: String): String {
        val serArgs = Json.decodeFromString(argsSerializer, args)
        return with(serArgs) {
            "Send $tonAmount TON to $receiverAddress"
        }
    }
}
