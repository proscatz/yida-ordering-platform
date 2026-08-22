package com.yida.messaging;

import com.yida.entity.Orders;
import com.yida.exception.OrderBusinessException;
import com.yida.mapper.MessageConsumeMapper;
import com.yida.mapper.OrderMapper;
import com.yida.service.support.OrderStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderTimeoutService {
    private final MessageConsumeMapper messageConsumeMapper;
    private final OrderMapper orderMapper;

    public OrderTimeoutService(MessageConsumeMapper messageConsumeMapper, OrderMapper orderMapper) {
        this.messageConsumeMapper = messageConsumeMapper;
        this.orderMapper = orderMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeIfPending(OrderCreatedEvent event) {
        int firstConsumption = messageConsumeMapper.insertIgnore(
                OrderMessagingConstants.TIMEOUT_CONSUMER, event.getEventId(), event.getOrderId(), LocalDateTime.now());
        if (firstConsumption == 0) return;

        Orders current = orderMapper.getById(event.getOrderId());
        if (current == null) throw new OrderBusinessException("订单不存在，超时消息等待人工核对");
        if (!Orders.PENDING_PAYMENT.equals(current.getStatus())) return;

        OrderStateMachine.requireTransition(Orders.PENDING_PAYMENT, Orders.CANCELLED);
        Orders update = Orders.builder()
                .id(current.getId())
                .status(Orders.CANCELLED)
                .cancelReason("订单支付超时")
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.updateByIdAndStatus(update, Orders.PENDING_PAYMENT);
    }
}