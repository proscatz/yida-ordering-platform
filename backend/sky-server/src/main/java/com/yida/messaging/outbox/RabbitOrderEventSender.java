package com.yida.messaging.outbox;

import com.alibaba.fastjson.JSON;
import com.yida.messaging.OrderCreatedEvent;
import com.yida.messaging.OrderMessagingConstants;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "yida.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitOrderEventSender implements OrderEventSender {
    private final RabbitTemplate rabbitTemplate;

    public RabbitOrderEventSender(RabbitTemplate rabbitTemplate, ConnectionFactory connectionFactory) {
        this.rabbitTemplate = rabbitTemplate;
        if (connectionFactory instanceof CachingConnectionFactory) {
            ((CachingConnectionFactory) connectionFactory)
                    .setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        }
    }

    @Override
    public void send(OrderOutbox outbox) throws Exception {
        OrderCreatedEvent event = JSON.parseObject(outbox.getPayload(), OrderCreatedEvent.class);
        long delayMillis = Math.max(1L, event.getTimeoutAtEpochMilli() - System.currentTimeMillis());
        CorrelationData correlationData = new CorrelationData(outbox.getEventId());
        rabbitTemplate.convertAndSend(
                OrderMessagingConstants.ORDER_DELAY_EXCHANGE,
                OrderMessagingConstants.ORDER_DELAY_ROUTING_KEY,
                outbox.getPayload(),
                message -> {
                    message.getMessageProperties().setExpiration(Long.toString(delayMillis));
                    message.getMessageProperties().setMessageId(outbox.getEventId());
                    message.getMessageProperties().setContentType("application/json");
                    return message;
                },
                correlationData);
        CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
        if (!confirm.isAck()) {
            throw new IllegalStateException("RabbitMQ 未确认订单事件: " + confirm.getReason());
        }
    }
}