package com.yida.messaging;

import com.yida.entity.Orders;
import com.yida.mapper.MessageConsumeMapper;
import com.yida.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutServiceTest {
    @Mock
    private MessageConsumeMapper messageConsumeMapper;
    @Mock
    private OrderMapper orderMapper;
    @InjectMocks
    private OrderTimeoutService service;

    @Test
    void duplicateMessageDoesNotReadOrMoveOrderAgain() {
        OrderCreatedEvent event = event();
        when(messageConsumeMapper.insertIgnore(eq(OrderMessagingConstants.TIMEOUT_CONSUMER),
                eq("event-1"), eq(10L), any())).thenReturn(0);

        service.closeIfPending(event);

        verify(orderMapper, never()).getById(any());
        verify(orderMapper, never()).updateByIdAndStatus(any(), any());
    }

    @Test
    void paidOrderIsRecordedAsConsumedButNeverCancelled() {
        when(messageConsumeMapper.insertIgnore(any(), any(), any(), any())).thenReturn(1);
        when(orderMapper.getById(10L)).thenReturn(Orders.builder()
                .id(10L).status(Orders.TO_BE_CONFIRMED).payStatus(Orders.PAID).build());

        service.closeIfPending(event());

        verify(orderMapper, never()).updateByIdAndStatus(any(), any());
    }

    @Test
    void pendingOrderIsCancelledWithAtomicExpectedStatus() {
        when(messageConsumeMapper.insertIgnore(any(), any(), any(), any())).thenReturn(1);
        when(orderMapper.getById(10L)).thenReturn(Orders.builder()
                .id(10L).status(Orders.PENDING_PAYMENT).payStatus(Orders.UN_PAID).build());
        when(orderMapper.updateByIdAndStatus(any(), eq(Orders.PENDING_PAYMENT))).thenReturn(1);

        service.closeIfPending(event());

        ArgumentCaptor<Orders> update = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).updateByIdAndStatus(update.capture(), eq(Orders.PENDING_PAYMENT));
        assertEquals(Orders.CANCELLED, update.getValue().getStatus());
    }

    @Test
    void concurrentPaymentWinningCasDoesNotCauseSecondTransition() {
        when(messageConsumeMapper.insertIgnore(any(), any(), any(), any())).thenReturn(1);
        when(orderMapper.getById(10L)).thenReturn(Orders.builder()
                .id(10L).status(Orders.PENDING_PAYMENT).build());
        when(orderMapper.updateByIdAndStatus(any(), eq(Orders.PENDING_PAYMENT))).thenReturn(0);

        service.closeIfPending(event());

        verify(orderMapper).updateByIdAndStatus(any(), eq(Orders.PENDING_PAYMENT));
    }

    private OrderCreatedEvent event() {
        return OrderCreatedEvent.builder().eventId("event-1").orderId(10L)
                .orderNumber("order-1").timeoutAtEpochMilli(System.currentTimeMillis()).build();
    }
}