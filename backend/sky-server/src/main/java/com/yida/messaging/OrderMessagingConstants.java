package com.yida.messaging;

public final class OrderMessagingConstants {
    public static final String ORDER_DELAY_EXCHANGE = "yida.order.delay.exchange";
    public static final String ORDER_DELAY_QUEUE = "yida.order.timeout.delay.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.created";
    public static final String ORDER_TIMEOUT_EXCHANGE = "yida.order.timeout.exchange";
    public static final String ORDER_TIMEOUT_QUEUE = "yida.order.timeout.queue";
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "order.timeout";
    public static final String ORDER_DEAD_EXCHANGE = "yida.order.dead.exchange";
    public static final String ORDER_DEAD_QUEUE = "yida.order.timeout.dead.queue";
    public static final String ORDER_DEAD_ROUTING_KEY = "order.timeout.dead";
    public static final String TIMEOUT_CONSUMER = "order-timeout-consumer";
    public static final String ORDER_CREATED_EVENT = "ORDER_CREATED";

    private OrderMessagingConstants() {
    }
}