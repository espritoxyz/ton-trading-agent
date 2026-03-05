package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.api.BlockchainAdapter
import com.agent.llm.tool.api.ConfirmationRequired
import com.agent.llm.tool.dto.CreateOrderArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

class CreateOrderAgentTool(
    private val bcAdapter: BlockchainAdapter
) : AgentTool<CreateOrderArgs>(), ConfirmationRequired {
    override val definition = ToolDefinition(
        name = "create_order",
        description = "Create an order for token specified by jettonMaster, with action specifying either sell or buy, and amount",
        argumentsSchema = ExplytJsonSchema(CreateOrderArgs::class)
    )

    override val argsSerializer = serializer<CreateOrderArgs>()

    override suspend fun payload(args: CreateOrderArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with $args" }
        val result = bcAdapter.createOrder(
            jettonMaster = args.jettonMaster,
            action = args.action,
            amount = args.amount,
            targetPrice = args.targetPrice,
            receivedJettonMaster = args.receivedJettonMaster,
            direction = args.direction,
        )

        return result
    }

    override fun confirmationText(args: String): String {
        val serArgs = Json.decodeFromString(argsSerializer, args)
        return with(serArgs) {
            val prettyAction = action.lowercase().replaceFirstChar { it.titlecase() }
            val temp = if (action == "buy") "spend" else "receive"
            "$prettyAction $amount $jettonTicker and $temp ${receivedJettonTicker ?: "TON"} when $jettonTicker price is $targetPrice USD"
        }
    }
}
