package com.agent.backend.rabbitmq.listeners

import com.agent.backend.llm.ChatJobService
import com.agent.backend.rabbitmq.RabbitConfig
import com.agent.backend.service.ExternalToolResultService
import com.agent.backend.service.NotificationEventPublisher
import com.agent.backend.service.WalletService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.util.*

@Component
class AgentEventsListener(
    private val jobService: ChatJobService,
    private val externalToolResultService: ExternalToolResultService,
    private val walletService: WalletService,
    private val notificationEventPublisher: NotificationEventPublisher,
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }



    @RabbitListener(queues = [RabbitConfig.QUEUE_AGENT_LLM])
    fun onEvent(@Payload payload: Map<String, Any?>) {
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

                    logger.info { "[agent-events] Processing send-ton result for user $userId: success=$success" }

                    // Record outgoing transaction if successful
                    if (success && txId != null && receiver != null) {
                        val amountNano = when (amount) {
                            is Number -> (amount.toDouble() * 1_000_000_000).toLong()
                            is String -> (amount.toDoubleOrNull() ?: 0.0 * 1_000_000_000).toLong()
                            else -> 0L
                        }

                        try {
                            walletService.processOutgoingTonTransaction(
                                userId = userId,
                                transactionHash = txId,
                                amountNano = amountNano,
                                recipientAddress = receiver
                            )
                        } catch (e: Exception) {
                            logger.error(e) { "[agent-events] Failed to record outgoing TON transaction" }
                        }
                    }

                    val report = if (success) {
                        if (txId != null) {
                            "TON transfer succeeded. Sent $amount TON to $receiver. https://tonviewer.com/transaction/$txId"
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

                    logger.info { "[agent-events] Successfully completed send-ton result for user $userId" }

                }
                "agent-llm.send-token.result" -> {
                    val messageId = (data["messageId"] as? String)?.let { UUID.fromString(it) } ?: return
                    val userId = (data["userId"] as? Number)?.toLong() ?: return
                    val success = data["success"] as? Boolean ?: false
                    val amount = data["tokenAmount"]
                    val amountNano = (data["tokenAmountNano"] as? Number)?.toLong()
                    val jettonMaster = data["jettonMaster"] as? String
                    val receiver = data["receiverAddress"] as? String
                    val txId = data["txId"] as? String
                    val error = data["error"] as? String

                    logger.info { "[agent-events] Processing send-token result for user $userId: success=$success" }

                    // Record outgoing token transaction if successful
                    if (success && txId != null && receiver != null && jettonMaster != null && amountNano != null) {
                        try {
                            walletService.processOutgoingTokenTransaction(
                                userId = userId,
                                transactionHash = txId,
                                amountNano = amountNano,
                                jettonMasterAddress = jettonMaster,
                                recipientAddress = receiver
                            )
                        } catch (e: Exception) {
                            logger.error(e) { "[agent-events] Failed to record outgoing token transaction" }
                        }
                    }

                    val report = if (success) {
                        if (txId != null) {
                            "Token transfer succeeded. Sent $amount of $jettonMaster to $receiver. https://tonviewer.com/transaction/$txId"
                        } else {
                            "Token transfer succeeded. Sent $amount of $jettonMaster to $receiver. (Transaction id unavailable)"
                        }
                    } else {
                        "Token transfer failed. Attempted to send $amount of $jettonMaster to $receiver. Error: $error."
                    }

                    externalToolResultService.complete(
                        messageId = messageId,
                        toolName = "send_token_to_address",
                        result = report,
                    )

                    logger.info { "[agent-events] Successfully completed send-token result for user $userId" }

                }
                "agent-llm.swap-ton-to-token.result" -> {
                    val messageId = (data["messageId"] as? String)?.let { UUID.fromString(it) } ?: return
                    val userId = (data["userId"] as? Number)?.toLong() ?: return
                    val success = data["success"] as? Boolean ?: false
                    val txId = data["txId"] as? String
                    val jettonMaster = data["requestedJettonMaster"] as? String
                    val swapTonAmount = data["requestedSwapTonAmount"] as? Number
                    val minimalTokenAmount = data["requestedMinimalTokenAmount"] as? Number
                    val error = data["error"] as? String

                    logger.info { "[agent-events] Processing swap-ton-to-token result for user $userId: success=$success" }

                    val report = if (success) {
                        if (txId != null) {
                            "Swap TON->Token succeeded. https://tonviewer.com/transaction/$txId"
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

                    // Emit notification event for successful swap
                    if (success) {
                        try {
                            notificationEventPublisher.publishNotificationEvent(
                                userId = userId,
                                type = "SWAP_EXECUTED",
                                title = "Swap Executed",
                                message = "Swapped ${swapTonAmount ?: "unknown"} TON for ${minimalTokenAmount ?: "unknown"} tokens",
                                metadata = mapOf(
                                    "fromAsset" to "TON",
                                    "toAsset" to (jettonMaster ?: "unknown"),
                                    "fromAmount" to (swapTonAmount?.toDouble() ?: 0.0),
                                    "toAmount" to (minimalTokenAmount?.toDouble() ?: 0.0),
                                    "transactionId" to (txId ?: "")
                                )
                            )
                        } catch (e: Exception) {
                            logger.warn(e) { "[agent-events] Failed to publish SWAP_EXECUTED notification" }
                        }
                    }

                    logger.info { "[agent-events] Successfully completed swap-ton-to-token result for user $userId" }

                }
                "agent-llm.swap-token-to-ton.result" -> {
                    val messageId = (data["messageId"] as? String)?.let { UUID.fromString(it) } ?: return
                    val userId = (data["userId"] as? Number)?.toLong() ?: return
                    val success = data["success"] as? Boolean ?: false
                    val txId = data["txId"] as? String
                    val jettonMaster = data["requestedJettonMaster"] as? String
                    val swapTokenAmount = data["requestedSwapTokenAmount"] as? Number
                    val minimalTonAmount = data["requestedMinimalTonAmount"] as? Number
                    val error = data["error"] as? String

                    logger.info { "[agent-events] Processing swap-token-to-ton result for user $userId: success=$success" }

                    val report = if (success) {
                        if (txId != null) {
                            "Swap Token->TON succeeded. https://tonviewer.com/transaction/$txId"
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

                    // Emit notification event for successful swap
                    if (success) {
                        try {
                            notificationEventPublisher.publishNotificationEvent(
                                userId = userId,
                                type = "SWAP_EXECUTED",
                                title = "Swap Executed",
                                message = "Swapped ${swapTokenAmount ?: "unknown"} tokens for ${minimalTonAmount ?: "unknown"} TON",
                                metadata = mapOf(
                                    "fromAsset" to (jettonMaster ?: "unknown"),
                                    "toAsset" to "TON",
                                    "fromAmount" to (swapTokenAmount?.toDouble() ?: 0.0),
                                    "toAmount" to (minimalTonAmount?.toDouble() ?: 0.0),
                                    "transactionId" to (txId ?: "")
                                )
                            )
                        } catch (e: Exception) {
                            logger.warn(e) { "[agent-events] Failed to publish SWAP_EXECUTED notification" }
                        }
                    }

                    logger.info { "[agent-events] Successfully completed swap-token-to-ton result for user $userId" }

                }
                else -> return

            }
        } catch (e: Exception) {
            logger.error(e) { "[agent-events] Failed to handle agent event" }
        }
    }
}
