package com.agent.backend.rabbitmq.listeners

import com.agent.backend.db.entity.NotificationType
import com.agent.backend.rabbitmq.RabbitConfig
import com.agent.backend.service.ExternalToolResultService
import com.agent.backend.service.NotificationEventPublisher
import com.agent.backend.service.NotificationService
import com.agent.backend.service.StonfiAssetsCacheService
import com.agent.backend.service.WalletService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class AgentEventsListener(
    private val externalToolResultService: ExternalToolResultService,
    private val walletService: WalletService,
    private val notificationEventPublisher: NotificationEventPublisher,
    private val notificationService: NotificationService,
    private val assetsCache: StonfiAssetsCacheService,
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
                    val rawSuccess = data["success"] as? Boolean ?: false
                    val error = data["error"] as? String
                    val success = error == null && rawSuccess
                    val amount = data["tonAmount"]
                    val receiver = data["receiverAddress"] as? String
                    val txId = data["txId"] as? String

                    logger.info { "[agent-events] Processing send-ton result for user $userId: success=$success, error=$error" }

                    // Record outgoing transaction if successful
                    if (success && txId != null && receiver != null) {
                        val amountNano = when (amount) {
                            is Number -> (amount.toDouble() * 1_000_000_000).toLong()
                            is String -> ((amount.toDoubleOrNull() ?: (0.0 * 1_000_000_000))).toLong()
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
                    val rawSuccess = data["success"] as? Boolean ?: false
                    val error = data["error"] as? String
                    val success = error == null && rawSuccess
                    val amount = data["tokenAmount"]
                    val amountNano = (data["tokenAmountNano"] as? Number)?.toLong()
                    val jettonMaster = data["jettonMaster"] as? String
                    val receiver = data["receiverAddress"] as? String
                    val txId = data["txId"] as? String

                    logger.info { "[agent-events] Processing send-token result for user $userId: success=$success, error=$error" }

                    // Record outgoing token transaction if successful
                    if (success && txId != null && receiver != null && jettonMaster != null && amountNano != null) {
                        try {
                            val asset = assetsCache.getAssetByContractAddress(jettonMaster)
                            walletService.processOutgoingTokenTransaction(
                                userId = userId,
                                transactionHash = txId,
                                amountNano = amountNano,
                                jettonMasterAddress = jettonMaster,
                                recipientAddress = receiver,
                                jettonSymbol = asset?.symbol,
                                jettonDecimals = asset?.decimals,
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
                    val rawSuccess = data["success"] as? Boolean ?: false
                    val error = data["error"] as? String
                    val success = error == null && rawSuccess
                    val txId = data["txId"] as? String
                    val jettonMaster = data["requestedJettonMaster"] as? String
                    val swapTonAmount = data["requestedSwapTonAmount"] as? Number
                    val minimalTokenAmount = data["requestedMinimalTokenAmount"] as? Number

                    logger.info { "[agent-events] Processing swap-ton-to-token result for user $userId: success=$success, error=$error" }

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

                    if (success) publishSwapTonToTokenNotification(
                        userId,
                        jettonMaster,
                        swapTonAmount,
                        minimalTokenAmount,
                        txId
                    )

                    logger.info { "[agent-events] Successfully completed swap-ton-to-token result for user $userId" }

                }

                "agent-llm.swap-token-to-ton.result" -> {
                    val messageId = (data["messageId"] as? String)?.let { UUID.fromString(it) } ?: return
                    val userId = (data["userId"] as? Number)?.toLong() ?: return
                    val rawSuccess = data["success"] as? Boolean ?: false
                    val error = data["error"] as? String
                    val success = error == null && rawSuccess
                    val txId = data["txId"] as? String
                    val jettonMaster = data["requestedJettonMaster"] as? String
                    val swapTokenAmountNano = data["requestedSwapTokenAmount"] as? Number
                    val minimalTonAmount = data["requestedMinimalTonAmount"] as? Number

                    logger.info { "[agent-events] Processing swap-token-to-ton result for user $userId: success=$success, error=$error" }

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

                    if (success) publishSwapTokenToTonNotification(
                        userId,
                        jettonMaster,
                        swapTokenAmountNano,
                        minimalTonAmount,
                        txId
                    )

                    logger.info { "[agent-events] Successfully completed swap-token-to-ton result for user $userId" }

                }

                "agent-llm.swap-token-to-token.result" -> {
                    val messageId = (data["messageId"] as? String)?.let { UUID.fromString(it) } ?: return
                    val userId = (data["userId"] as? Number)?.toLong() ?: return
                    val rawSuccess = data["success"] as? Boolean ?: false
                    val error = data["error"] as? String
                    val success = error == null && rawSuccess
                    val txId = data["txId"] as? String
                    val offerJettonMaster = data["requestedOfferJettonMaster"] as? String
                    val askJettonMaster = data["requestedAskJettonMaster"] as? String
                    val swapOfferTokenAmountNano = data["requestedSwapOfferTokenAmount"] as? Number
                    val askTokenAmount = data["askNano"] as? Number

                    logger.info { "[agent-events] Processing swap-token-to-token result for user $userId: success=$success, error=$error" }

                    val report = if (success) {
                        if (txId != null) {
                            "Swap Token->Token succeeded. https://tonviewer.com/transaction/$txId"
                        } else {
                            "Swap Token->Token succeeded. (Transaction id unavailable)"
                        }
                    } else {
                        "Swap Token->Token failed. Error: $error."
                    }

                    externalToolResultService.complete(
                        messageId = messageId,
                        toolName = "swap_token_to_token",
                        result = report,
                    )

                    if (success) publishSwapTokenToTokenNotification(
                        userId = userId,
                        offerJettonMaster = offerJettonMaster,
                        askJettonMaster = askJettonMaster,
                        swapOfferTokenAmountNano = swapOfferTokenAmountNano,
                        askTokenAmountNano = askTokenAmount,
                        txId = txId,
                    )

                    logger.info { "[agent-events] Successfully completed swap-token-to-token result for user $userId" }

                }

                else -> return
            }
        } catch (e: Exception) {
            logger.error(e) { "[agent-events] Failed to handle agent event" }
        }
    }

    private fun publishSwapTonToTokenNotification(
        userId: Long,
        jettonMaster: String?,
        swapTonAmount: Number?,
        minimalTokenAmount: Number?,
        txId: String?,
    ) {
        try {
            val asset = jettonMaster?.let { assetsCache.getAssetByContractAddress(it) }
            val tokenSymbol = asset?.symbol ?: jettonMaster ?: "unknown"
            val swapTonAmountHuman = swapTonAmount?.toString() ?: "unknown"
            val minimalTokenAmountHuman = minimalTokenAmount?.toString() ?: "unknown"
            val metadata = mapOf<String, Any>(
                "fromAsset" to "TON",
                "toAsset" to tokenSymbol,
                "fromAmount" to swapTonAmountHuman,
                "toAmount" to minimalTokenAmountHuman,
                "transactionId" to (txId ?: "")
            )
            val (title, message) = notificationService.generateNotificationText(
                NotificationType.SWAP_EXECUTED,
                metadata
            )
            notificationEventPublisher.publishNotificationEvent(
                userId = userId,
                type = "SWAP_EXECUTED",
                title = title,
                message = message,
                metadata = metadata
            )
        } catch (e: Exception) {
            logger.warn(e) { "[agent-events] Failed to publish SWAP_EXECUTED notification" }
        }
    }

    private fun publishSwapTokenToTonNotification(
        userId: Long,
        jettonMaster: String?,
        swapTokenAmountNano: Number?,
        minimalTonAmount: Number?,
        txId: String?,
    ) {
        try {
            val asset = jettonMaster?.let { assetsCache.getAssetByContractAddress(it) }
            val tokenSymbol = asset?.symbol ?: jettonMaster ?:  "unknown"
            val decimals = asset?.decimals ?: 9
            val swapTokenAmountHuman = swapTokenAmountNano?.let { nano ->
                BigDecimal(nano.toLong())
                    .divide(BigDecimal.TEN.pow(decimals), decimals, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString()
            } ?: "unknown"
            val metadata = mapOf<String, Any>(
                "fromAsset" to tokenSymbol,
                "toAsset" to "TON",
                "fromAmount" to swapTokenAmountHuman,
                "toAmount" to (minimalTonAmount?.toString() ?: "unknown"),
                "transactionId" to (txId ?: "")
            )
            val (title, message) = notificationService.generateNotificationText(
                NotificationType.SWAP_EXECUTED,
                metadata
            )
            notificationEventPublisher.publishNotificationEvent(
                userId = userId,
                type = "SWAP_EXECUTED",
                title = title,
                message = message,
                metadata = metadata
            )
        } catch (e: Exception) {
            logger.warn(e) { "[agent-events] Failed to publish SWAP_EXECUTED notification" }
        }
    }

    private fun publishSwapTokenToTokenNotification(
        userId: Long,
        offerJettonMaster: String?,
        askJettonMaster: String?,
        swapOfferTokenAmountNano: Number?,
        askTokenAmountNano: Number?,
        txId: String?,
    ) {
        try {
            val offerAsset = offerJettonMaster?.let { assetsCache.getAssetByContractAddress(it) }
            val askAsset = askJettonMaster?.let { assetsCache.getAssetByContractAddress(it) }
            val offerSymbol = offerAsset?.symbol ?: offerJettonMaster ?: "unknown"
            val askSymbol = askAsset?.symbol ?: askJettonMaster ?: "unknown"

            val offerDecimals = offerAsset?.decimals ?: 9
            val askDecimals = askAsset?.decimals ?: 9
            val offerAmountHuman = swapOfferTokenAmountNano?.let { nano ->
                BigDecimal(nano.toLong())
                    .divide(BigDecimal.TEN.pow(offerDecimals), offerDecimals, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString()
            } ?: "unknown"

            val askAmountHuman = askTokenAmountNano?.let { nano ->
                BigDecimal(nano.toLong())
                    .divide(BigDecimal.TEN.pow(askDecimals), askDecimals, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString()
            } ?: "unknown"

            val metadata = mapOf<String, Any>(
                "fromAsset" to offerSymbol,
                "toAsset" to askSymbol,
                "fromAmount" to offerAmountHuman,
                "toAmount" to askAmountHuman,
                "transactionId" to (txId ?: ""),
            )

            val (title, message) = notificationService.generateNotificationText(
                NotificationType.SWAP_EXECUTED,
                metadata,
            )
            notificationEventPublisher.publishNotificationEvent(
                userId = userId,
                type = "SWAP_EXECUTED",
                title = title,
                message = message,
                metadata = metadata,
            )
        } catch (e: Exception) {
            logger.warn(e) { "[agent-events] Failed to publish SWAP_EXECUTED notification" }
        }
    }
}

