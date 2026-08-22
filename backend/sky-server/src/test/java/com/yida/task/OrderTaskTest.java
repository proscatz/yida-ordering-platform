package com.yida.task;

import com.yida.entity.Orders;
import com.yida.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTaskTest {

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderTask orderTask;

    @Test
    void timeoutCloseUsesPendingStatusAsAtomicPrecondition() {
        Orders pending = Orders.builder().id(1L).status(Orders.PENDING_PAYMENT).build();
        when(orderMapper.getByStatusAndOrderTime(eq(Orders.PENDING_PAYMENT), any()))
                .thenReturn(Collections.singletonList(pending));
        when(orderMapper.updateByIdAndStatus(any(), eq(Orders.PENDING_PAYMENT))).thenReturn(1);

        orderTask.processTimeoutOrder();

        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).updateByIdAndStatus(captor.capture(), eq(Orders.PENDING_PAYMENT));
        assertEquals(Orders.CANCELLED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getCancelTime());
    }

    @Test
    void autoCompleteChecksAffectedRowsWhenAnotherWorkerWins() {
        Orders delivering = Orders.builder().id(1L).status(Orders.DELIVERY_IN_PROGRESS).build();
        when(orderMapper.getByStatusAndOrderTime(eq(Orders.DELIVERY_IN_PROGRESS), any()))
                .thenReturn(Collections.singletonList(delivering));
        when(orderMapper.updateByIdAndStatus(any(), eq(Orders.DELIVERY_IN_PROGRESS))).thenReturn(0);

        orderTask.processDeliveryOrder();

        verify(orderMapper).updateByIdAndStatus(any(), eq(Orders.DELIVERY_IN_PROGRESS));
    }
}