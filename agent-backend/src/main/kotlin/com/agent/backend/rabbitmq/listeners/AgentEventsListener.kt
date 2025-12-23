package com.agent.backend.rabbitmq.listeners

import com.agent.backend.llm.ChatJobService
import com.agent.backend.rabbitmq.RabbitConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class AgentEventsListener(
    private val jobService: ChatJobService
) {
    data class SendTonResult(
        val type: String,
        val occurredAt: String,
        val correlation: Map<String, Any?>? = null,
        val data: Data
    ) {
        data class Data(
            val messageId: String?,
            val userId: Long?,
            val tonAmount: Any?,
            val receiverAddress: String?,
            val success: Boolean,
            val txId: String? = null,
            val error: String? = null
        )
    }

    @RabbitListener(queues = [RabbitConfig.QUEUE])
    fun onEvent(@Payload payload: Map<String, Any?>) {
        logger.debug {
            "Received rabbitmq event ${payload.entries.joinToString(prefix="{", postfix="}") { 
                "${it.key}=${it.value.toString()}" 
            }}"
        }
        try {
            val type = payload["type"] as? String ?: return
            if (type != "agent-llm.send-ton.result") return
            val data = (payload["data"] as? Map<*, *>) ?: return
            val messageId = (data["messageId"] as? String)?.let { UUID.fromString(it) } ?: return
            val userId = (data["userId"] as? Number)?.toLong() ?: return
            val success = data["success"] as? Boolean ?: false
            val amount = data["tonAmount"]
            val receiver = data["receiverAddress"] as? String
            val txId = data["txId"] as? String
            val error = data["error"] as? String

            val report = if (success) {
                "TON transfer succeeded. Sent $amount TON to $receiver. Transaction ID: $txId."
            } else {
                "TON transfer failed. Attempted to send $amount TON to $receiver. Error: $error."
            }

            runBlocking {
                jobService.finalizeWithToolResult(
                    messageId = messageId,
                    userId = userId,
                    toolName = "send_ton_to_address",
                    toolResult = report
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to handle agent event: $payload" }
        }
    }
}
