package com.yida.service.support;

import com.yida.entity.Orders;
import com.yida.exception.OrderBusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStateMachineTest {

    @Test
    void allowsDefinedMainFlowAndCancellationTransitions() {
        assertTrue(OrderStateMachine.canTransition(Orders.PENDING_PAYMENT, Orders.TO_BE_CONFIRMED));
        assertTrue(OrderStateMachine.canTransition(Orders.TO_BE_CONFIRMED, Orders.CONFIRMED));
        assertTrue(OrderStateMachine.canTransition(Orders.CONFIRMED, Orders.DELIVERY_IN_PROGRESS));
        assertTrue(OrderStateMachine.canTransition(Orders.DELIVERY_IN_PROGRESS, Orders.COMPLETED));
        assertDoesNotThrow(() -> OrderStateMachine.requireTransition(Orders.CONFIRMED, Orders.CANCELLED));
    }

    @Test
    void rejectsSkippedBackwardAndTerminalTransitions() {
        assertFalse(OrderStateMachine.canTransition(Orders.TO_BE_CONFIRMED, Orders.COMPLETED));
        assertThrows(OrderBusinessException.class,
                () -> OrderStateMachine.requireTransition(Orders.COMPLETED, Orders.CANCELLED));
        assertThrows(OrderBusinessException.class,
                () -> OrderStateMachine.requireTransition(Orders.CANCELLED, Orders.CONFIRMED));
    }
}