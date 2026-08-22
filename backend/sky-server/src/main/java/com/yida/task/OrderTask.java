package com.yida.task;

import com.yida.entity.Orders;
import com.yida.mapper.OrderMapper;
import com.yida.service.support.OrderStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    @Scheduled(cron = "0 */10 * * * ?")
    public void processTimeoutOrder() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(15);
        List<Orders> orders = orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT, deadline);
        if (orders == null) {
            return;
        }
        for (Orders current : orders) {
            OrderStateMachine.requireTransition(current.getStatus(), Orders.CANCELLED);
            Orders update = Orders.builder()
                    .id(current.getId())
                    .status(Orders.CANCELLED)
                    .cancelReason("订单支付超时")
                    .cancelTime(LocalDateTime.now())
                    .build();
            int affectedRows = orderMapper.updateByIdAndStatus(update, Orders.PENDING_PAYMENT);
            if (affectedRows != 1) {
                log.info("跳过已被并发处理的超时订单，orderId={}", current.getId());
            }
        }
    }

    @Scheduled(cron = "0 * * * * ?")
    public void processDeliveryOrder() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(60);
        List<Orders> orders = orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS, deadline);
        if (orders == null) {
            return;
        }
        for (Orders current : orders) {
            OrderStateMachine.requireTransition(current.getStatus(), Orders.COMPLETED);
            Orders update = Orders.builder()
                    .id(current.getId())
                    .status(Orders.COMPLETED)
                    .deliveryTime(LocalDateTime.now())
                    .build();
            int affectedRows = orderMapper.updateByIdAndStatus(update, Orders.DELIVERY_IN_PROGRESS);
            if (affectedRows != 1) {
                log.info("跳过已被并发处理的配送订单，orderId={}", current.getId());
            }
        }
    }
}