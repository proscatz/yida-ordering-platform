package com.yida.config;

import com.yida.messaging.OrderMessagingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
@ConditionalOnProperty(name = "yida.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitOrderConfiguration {

    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(OrderMessagingConstants.ORDER_DELAY_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(OrderMessagingConstants.ORDER_DELAY_QUEUE)
                .deadLetterExchange(OrderMessagingConstants.ORDER_TIMEOUT_EXCHANGE)
                .deadLetterRoutingKey(OrderMessagingConstants.ORDER_TIMEOUT_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding orderDelayBinding(Queue orderDelayQueue, DirectExchange orderDelayExchange) {
        return BindingBuilder.bind(orderDelayQueue).to(orderDelayExchange)
                .with(OrderMessagingConstants.ORDER_DELAY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange orderTimeoutExchange() {
        return new DirectExchange(OrderMessagingConstants.ORDER_TIMEOUT_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(OrderMessagingConstants.ORDER_TIMEOUT_QUEUE).build();
    }

    @Bean
    public Binding orderTimeoutBinding(Queue orderTimeoutQueue, DirectExchange orderTimeoutExchange) {
        return BindingBuilder.bind(orderTimeoutQueue).to(orderTimeoutExchange)
                .with(OrderMessagingConstants.ORDER_TIMEOUT_ROUTING_KEY);
    }

    @Bean
    public DirectExchange orderDeadExchange() {
        return new DirectExchange(OrderMessagingConstants.ORDER_DEAD_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderDeadQueue() {
        return QueueBuilder.durable(OrderMessagingConstants.ORDER_DEAD_QUEUE).build();
    }

    @Bean
    public Binding orderDeadBinding(Queue orderDeadQueue, DirectExchange orderDeadExchange) {
        return BindingBuilder.bind(orderDeadQueue).to(orderDeadExchange)
                .with(OrderMessagingConstants.ORDER_DEAD_ROUTING_KEY);
    }

    @Bean
    public RetryOperationsInterceptor orderMessageRetryInterceptor(RabbitTemplate rabbitTemplate) {
        RepublishMessageRecoverer recoverer = new RepublishMessageRecoverer(
                rabbitTemplate, OrderMessagingConstants.ORDER_DEAD_EXCHANGE,
                OrderMessagingConstants.ORDER_DEAD_ROUTING_KEY);
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(500, 2.0, 5000)
                .recoverer(recoverer)
                .build();
    }

    @Bean("orderRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory orderRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, RetryOperationsInterceptor orderMessageRetryInterceptor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(orderMessageRetryInterceptor);
        return factory;
    }
}