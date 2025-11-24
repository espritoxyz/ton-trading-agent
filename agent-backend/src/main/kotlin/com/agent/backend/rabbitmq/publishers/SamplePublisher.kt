package com.agent.backend.rabbitmq.publishers

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

//@Component
//class SamplePublisher(private val rabbitTemplate: RabbitTemplate) : CommandLineRunner {
//    override fun run(vararg args: String?) {
//        val payload = mapOf(
//            "type" to "user.created",
//            "messageId" to UUID.randomUUID().toString(),
//            "occurredAt" to Instant.now().toString(),
//            "data" to mapOf("id" to UUID.randomUUID().toString(), "email" to "kotlin@example.com")
//        )
////        rabbitTemplate.convertAndSend("app.events", "user.created", payload)
//        logger.info { "[agent-backend] published user.created" }
//    }
//}
