package com.agent.backend.rabbitmq

import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfig {

    companion object {
        const val EXCHANGE = "app.events"
        const val QUEUE = "agent-backend.in"
        const val ROUTING_PATTERN = "user.*"
    }

    @Bean
    fun connectionFactory(): CachingConnectionFactory {
        val uri = System.getenv("RABBIT_URL") ?: "amqp://guest:guest@localhost:5672/"
        return CachingConnectionFactory().apply { setUri(uri) }
    }

    @Bean
    fun messageConverter() = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitTemplate(cf: CachingConnectionFactory): RabbitTemplate =
        RabbitTemplate(cf).apply {
            messageConverter = messageConverter()
            isChannelTransacted = false
            setMandatory(true)
        }

    @Bean fun exchange(): TopicExchange =
        ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build()

    @Bean fun queue(): Queue =
        QueueBuilder.durable(QUEUE).build()

    @Bean fun binding(): Binding =
        BindingBuilder.bind(queue()).to(exchange()).with(ROUTING_PATTERN)
}
