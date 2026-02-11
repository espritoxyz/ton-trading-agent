package com.agent.backend.rabbitmq.listeners

import com.agent.backend.rabbitmq.RabbitConfig
import com.agent.backend.service.DepositService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class DepositEventsListener(
    private val depositService: DepositService
) {

    @RabbitListener(queues = [RabbitConfig.QUEUE_DEPOSIT])
    fun onDepositEvent(@Payload payload: Map<String, Any?>) {
        try {
            val type = payload["type"] as? String ?: return

            when (type) {
                "deposit.transaction-found" -> {
                    logger.debug { "Received deposit.transaction-found event: $payload" }

                    val data = (payload["data"] as? Map<*, *>) ?: run {
                        logger.warn { "Missing data in deposit.transaction-found event" }
                        return
                    }

                    val transactionHash = data["transactionHash"] as? String ?: run {
                        logger.warn { "Missing transactionHash in deposit event" }
                        return
                    }

                    val transactionLt = (data["transactionLt"] as? String)?.toLongOrNull() ?: run {
                        logger.warn { "Missing or invalid transactionLt in deposit event" }
                        return
                    }

                    val bodyHash = data["bodyHash"] as? String ?: run {
                        logger.warn { "Missing bodyHash in deposit event" }
                        return
                    }

                    val comment = data["comment"] as? String ?: run {
                        logger.warn { "Missing comment in deposit event" }
                        return
                    }

                    val amountNano = (data["amountNano"] as? String)?.toLongOrNull() ?: run {
                        logger.warn { "Missing or invalid amountNano in deposit event" }
                        return
                    }

                    val sender = data["sender"] as? String
                    val assetType = data["assetType"] as? String ?: "TON"
                    val jettonMasterAddress = data["jettonMasterAddress"] as? String
                    val jettonSymbol = data["jettonSymbol"] as? String
                    val jettonDecimals = (data["jettonDecimals"] as? Number)?.toInt()

                    // Validate code format (6 characters, uppercase alphanumeric)
                    val normalizedCode = comment.trim().uppercase()
                    if (!normalizedCode.matches(Regex("^[A-Z0-9]{6}$"))) {
                        logger.info { "Invalid deposit code format: $comment (normalized: $normalizedCode)" }
                        return
                    }

                    val assetDisplay = when (assetType) {
                        "TON" -> "${amountNano / 1_000_000_000.0} TON"
                        "JETTON" -> {
                            val decimals = jettonDecimals ?: 9
                            val amount = amountNano / Math.pow(10.0, decimals.toDouble())
                            "$amount ${jettonSymbol ?: "JETTON"}"
                        }
                        else -> "${amountNano}nano"
                    }

                    logger.info {
                        "Processing deposit transaction: code=$normalizedCode, amount=$assetDisplay, " +
                        "assetType=$assetType, tx=$transactionHash, lt=$transactionLt, sender=$sender"
                    }

                    try {
                        depositService.processDepositDetected(
                            code = normalizedCode,
                            transactionHash = transactionHash,
                            transactionLt = transactionLt,
                            bodyHash = bodyHash,
                            amountNano = amountNano,
                            assetType = assetType,
                            jettonMasterAddress = jettonMasterAddress,
                            jettonSymbol = jettonSymbol,
                            jettonDecimals = jettonDecimals
                        )

                        logger.info {
                            "Successfully processed deposit for code $normalizedCode: $assetDisplay"
                        }
                    } catch (e: IllegalArgumentException) {
                        logger.warn { "Failed to process deposit: ${e.message}" }
                    } catch (e: Exception) {
                        logger.error(e) { "Error processing deposit transaction: $transactionHash" }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to handle deposit event: $payload" }
        }
    }
}
