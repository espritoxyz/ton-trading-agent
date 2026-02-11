package com.agent.backend.rabbitmq

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.ExchangeBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableRabbit
class RabbitConfig {

    companion object {
        const val EXCHANGE = "app.events"
        const val QUEUE = "agent-backend"
        // Match both agent-llm.* and deposit.* patterns
        const val ROUTING_PATTERN_1 = "agent-llm.#"
        const val ROUTING_PATTERN_2 = "deposit.#"
    }

    @Bean
    fun connectionFactory(): CachingConnectionFactory {
        val uri = System.getenv("RABBIT_URL")
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

    @Bean fun binding1(): Binding =
        BindingBuilder.bind(queue()).to(exchange()).with(ROUTING_PATTERN_1)

    @Bean fun binding2(): Binding =
        BindingBuilder.bind(queue()).to(exchange()).with(ROUTING_PATTERN_2)
}
