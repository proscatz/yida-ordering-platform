package com.yida.messaging;

import com.alibaba.fastjson.JSON;
import com.yida.mapper.MqDeadLetterMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "yida.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class OrderTimeoutDeadLetterConsumer {
    private final MqDeadLetterMapper deadLetterMapper;

    public OrderTimeoutDeadLetterConsumer(MqDeadLetterMapper deadLetterMapper) {
        this.deadLetterMapper = deadLetterMapper;
    }

    @RabbitListener(queues = OrderMessagingConstants.ORDER_DEAD_QUEUE)
    public void consume(String payload) {
        OrderCreatedEvent event = JSON.parseObject(payload, OrderCreatedEvent.class);
        String eventId = event != null && event.getEventId() != null
                ? event.getEventId() : "invalid-" + Integer.toHexString(payload.hashCode());
        deadLetterMapper.insertIgnore(eventId, OrderMessagingConstants.ORDER_DEAD_QUEUE,
                payload, LocalDateTime.now());
    }
}