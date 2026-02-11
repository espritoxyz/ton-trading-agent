package com.agent.backend.rabbitmq

import org.springframework.amqp.core.*
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

        // Separate queues for each event type
        const val QUEUE_AGENT_LLM = "agent-backend.agent-llm"
        const val QUEUE_DEPOSIT = "agent-backend.deposit"
        const val QUEUE_WALLET = "agent-backend.wallet"

        // Routing patterns
        const val ROUTING_PATTERN_AGENT_LLM = "agent-llm.#"
        const val ROUTING_PATTERN_DEPOSIT = "deposit.#"
        const val ROUTING_PATTERN_WALLET = "wallet.#"
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

    // Agent LLM Queue and Binding
    @Bean fun queueAgentLlm(): Queue =
        QueueBuilder.durable(QUEUE_AGENT_LLM).build()

    @Bean fun bindingAgentLlm(queueAgentLlm: Queue, exchange: TopicExchange): Binding =
        BindingBuilder.bind(queueAgentLlm).to(exchange).with(ROUTING_PATTERN_AGENT_LLM)

    // Deposit Queue and Binding
    @Bean fun queueDeposit(): Queue =
        QueueBuilder.durable(QUEUE_DEPOSIT).build()

    @Bean fun bindingDeposit(queueDeposit: Queue, exchange: TopicExchange): Binding =
        BindingBuilder.bind(queueDeposit).to(exchange).with(ROUTING_PATTERN_DEPOSIT)

    // Wallet Queue and Binding
    @Bean fun queueWallet(): Queue =
        QueueBuilder.durable(QUEUE_WALLET).build()

    @Bean fun bindingWallet(queueWallet: Queue, exchange: TopicExchange): Binding =
        BindingBuilder.bind(queueWallet).to(exchange).with(ROUTING_PATTERN_WALLET)
}
