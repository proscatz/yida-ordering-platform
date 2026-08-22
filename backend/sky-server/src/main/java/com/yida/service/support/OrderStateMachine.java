package com.yida.service.support;

import com.yida.constant.MessageConstant;
import com.yida.entity.Orders;
import com.yida.exception.OrderBusinessException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机，集中定义允许的状态迁移。
 */
public final class OrderStateMachine {

    private static final Map<Integer, Set<Integer>> ALLOWED_TRANSITIONS;

    static {
        Map<Integer, Set<Integer>> transitions = new HashMap<>();
        transitions.put(Orders.PENDING_PAYMENT, states(Orders.TO_BE_CONFIRMED, Orders.CANCELLED));
        transitions.put(Orders.TO_BE_CONFIRMED, states(Orders.CONFIRMED, Orders.CANCELLED));
        transitions.put(Orders.CONFIRMED, states(Orders.DELIVERY_IN_PROGRESS, Orders.CANCELLED));
        transitions.put(Orders.DELIVERY_IN_PROGRESS, states(Orders.COMPLETED, Orders.CANCELLED));
        transitions.put(Orders.COMPLETED, Collections.emptySet());
        transitions.put(Orders.CANCELLED, Collections.emptySet());
        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    private OrderStateMachine() {
    }

    public static boolean canTransition(Integer currentStatus, Integer targetStatus) {
        Set<Integer> targets = ALLOWED_TRANSITIONS.get(currentStatus);
        return targets != null && targets.contains(targetStatus);
    }

    public static void requireTransition(Integer currentStatus, Integer targetStatus) {
        if (!canTransition(currentStatus, targetStatus)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    private static Set<Integer> states(Integer... statuses) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(statuses)));
    }
}