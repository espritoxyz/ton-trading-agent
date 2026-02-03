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

    @RabbitListener(queues = [RabbitConfig.QUEUE])
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

                    val amountTonNano = (data["amountTonNano"] as? String)?.toLongOrNull() ?: run {
                        logger.warn { "Missing or invalid amountTonNano in deposit event" }
                        return
                    }

                    val sender = data["sender"] as? String

                    // Validate code format (6 characters, uppercase alphanumeric)
                    val normalizedCode = comment.trim().uppercase()
                    if (!normalizedCode.matches(Regex("^[A-Z0-9]{6}$"))) {
                        logger.info { "Invalid deposit code format: $comment (normalized: $normalizedCode)" }
                        return
                    }

                    logger.info {
                        "Processing deposit transaction: code=$normalizedCode, amount=${amountTonNano}nano, " +
                        "tx=$transactionHash, lt=$transactionLt, sender=$sender"
                    }

                    try {
                        depositService.processDepositDetected(
                            code = normalizedCode,
                            transactionHash = transactionHash,
                            transactionLt = transactionLt,
                            bodyHash = bodyHash,
                            amountTonNano = amountTonNano
                        )

                        logger.info {
                            "Successfully processed deposit for code $normalizedCode: " +
                            "${amountTonNano / 1_000_000_000.0} TON"
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
