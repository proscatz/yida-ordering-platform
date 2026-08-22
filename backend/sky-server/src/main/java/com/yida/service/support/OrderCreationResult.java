package com.yida.service.support;

import com.yida.entity.Orders;

public class OrderCreationResult {

    private final Orders order;
    private final boolean created;

    private OrderCreationResult(Orders order, boolean created) {
        this.order = order;
        this.created = created;
    }

    public static OrderCreationResult created(Orders order) {
        return new OrderCreationResult(order, true);
    }

    public static OrderCreationResult existing(Orders order) {
        return new OrderCreationResult(order, false);
    }

    public Orders getOrder() {
        return order;
    }

    public boolean isCreated() {
        return created;
    }
}