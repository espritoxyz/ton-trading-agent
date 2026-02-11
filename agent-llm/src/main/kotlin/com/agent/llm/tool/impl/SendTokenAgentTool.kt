package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.api.ConfirmationRequired
import com.agent.llm.tool.dto.SendTokenArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class SendTokenAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<SendTokenArgs>(), ConfirmationRequired {
    override val definition = ToolDefinition(
        name = "send_token_to_address",
        description = "Send specified amount of token to given address and return action result ${additionalDescriptionText()}",
        argumentsSchema = ExplytJsonSchema(SendTokenArgs::class)
    )

    override val argsSerializer = serializer<SendTokenArgs>()

    override fun payload(args: SendTokenArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        bcAdapter.sendTokenToAddress(args.tokenAmount, args.jettonMaster, args.receiverAddress)

        return "Transfer to ${args.receiverAddress} of ${args.tokenAmount} ${args.jettonMaster} initiated"
    }

    override fun confirmationText(args: String): String {
        val serArgs = Json.decodeFromString(argsSerializer, args)
        return with(serArgs) {
            "Send $tokenAmount of $jettonMaster to $receiverAddress"
        }
    }
}
