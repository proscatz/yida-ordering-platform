package com.yida.messaging;

import com.alibaba.fastjson.JSON;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "yida.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class OrderTimeoutConsumer {
    private final OrderTimeoutService orderTimeoutService;

    public OrderTimeoutConsumer(OrderTimeoutService orderTimeoutService) {
        this.orderTimeoutService = orderTimeoutService;
    }

    @RabbitListener(queues = OrderMessagingConstants.ORDER_TIMEOUT_QUEUE,
            containerFactory = "orderRabbitListenerContainerFactory")
    public void consume(String payload) {
        OrderCreatedEvent event = JSON.parseObject(payload, OrderCreatedEvent.class);
        if (event == null || event.getEventId() == null || event.getOrderId() == null) {
            throw new IllegalArgumentException("无效的订单超时消息");
        }
        orderTimeoutService.closeIfPending(event);
    }
}