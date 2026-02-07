package com.agent.backend.rabbitmq.listeners

import com.agent.backend.llm.ChatJobService
import com.agent.backend.rabbitmq.RabbitConfig
import com.agent.backend.service.ExternalToolResultService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID

import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class AgentEventsListener(
    private val jobService: ChatJobService,
    private val externalToolResultService: ExternalToolResultService,
) {


    @RabbitListener(queues = [RabbitConfig.QUEUE])
    fun onEvent(@Payload payload: Map<String, Any?>) {
        logger.debug {
            "Received rabbitmq event ${payload.entries.joinToString(prefix="{", postfix="}") { 
                "${it.key}=${it.value.toString()}" 
            }}"
        }
        try {
            val type = payload["type"] as? String ?: return
            val data = (payload["data"] as? Map<*, *>) ?: return

            when (type) {
                "agent-llm.send-ton.result" -> {
                    val messageId = (data["messageId"] as? String)?.let { UUID.fromString(it) } ?: return
                    val userId = (data["userId"] as? Number)?.toLong() ?: return
                    val success = data["success"] as? Boolean ?: false
                    val amount = data["tonAmount"]
                    val receiver = data["receiverAddress"] as? String
                    val txId = data["txId"] as? String
                    val error = data["error"] as? String

                    val report = if (success) {
                        val txLink = txId?.let { "<a href=\"https://tonviewer.com/transaction/$it\" target=\"_blank\" rel=\"noopener noreferrer\">https://tonviewer.com/transaction/$it</a>" }
                        if (txLink != null) {
                            "TON transfer succeeded. Sent $amount TON to $receiver. Transaction: $txLink"
                        } else {
                            "TON transfer succeeded. Sent $amount TON to $receiver. (Transaction id unavailable)"
                        }
                    } else {
                        "TON transfer failed. Attempted to send $amount TON to $receiver. Error: $error."
                    }

                    externalToolResultService.complete(
                        messageId = messageId,
                        toolName = "send_ton_to_address",
                        result = report,
                    )

                }
                "agent-llm.swap-ton-to-token.result" -> {
                    val messageId = (data["messageId"] as? String)?.let { UUID.fromString(it) } ?: return
                    val userId = (data["userId"] as? Number)?.toLong() ?: return
                    val success = data["success"] as? Boolean ?: false
                    val txId = data["txId"] as? String
//                    val jettonMinter = data["jettonMinter"] as? String
//                    val offerNanotons = data["offerNanotons"] as? String
//                    val minAskNano = data["minAskNano"] as? String
//                    val router = data["router"] as? String
//                    val pool = data["pool"] as? String
//                    val pTon = data["pTon"] as? String
                    val error = data["error"] as? String

                    val report = if (success) {
                        val txLink = txId?.let { "<a href=\"https://tonviewer.com/transaction/$it\" target=\"_blank\" rel=\"noopener noreferrer\">https://tonviewer.com/transaction/$it</a>" }
                        if (txLink != null) {
                            "Swap TON->Token succeeded. Transaction: $txLink"
                        } else {
                            "Swap TON->Token succeeded. (Transaction id unavailable)"
                        }
                    } else {
                        "Swap TON->Token failed. Error: $error."
                    }

                    externalToolResultService.complete(
                        messageId = messageId,
                        toolName = "swap_ton_to_token",
                        result = report,
                    )

                }
                "agent-llm.swap-token-to-ton.result" -> {
                    val messageId = (data["messageId"] as? String)?.let { UUID.fromString(it) } ?: return
                    val userId = (data["userId"] as? Number)?.toLong() ?: return
                    val success = data["success"] as? Boolean ?: false
                    val txId = data["txId"] as? String
//                    val jettonMinter = data["jettonMinter"] as? String
//                    val offerNanotons = data["offerNanotons"] as? String
//                    val minAskNano = data["minAskNano"] as? String
//                    val router = data["router"] as? String
//                    val pool = data["pool"] as? String
//                    val pTon = data["pTon"] as? String
                    val error = data["error"] as? String

                    val report = if (success) {
                        val txLink = txId?.let { "<a href=\"https://tonviewer.com/transaction/$it\" target=\"_blank\" rel=\"noopener noreferrer\">https://tonviewer.com/transaction/$it</a>" }
                        if (txLink != null) {
                            "Swap Token->TON succeeded. Transaction: $txLink"
                        } else {
                            "Swap Token->TON succeeded. (Transaction id unavailable)"
                        }
                    } else {
                        "Swap Token->TON failed. Error: $error."
                    }
                    externalToolResultService.complete(
                        messageId = messageId,
                        toolName = "swap_token_to_ton",
                        result = report,
                    )

                }
                else -> return

            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to handle agent event: $payload" }
        }
    }
}
