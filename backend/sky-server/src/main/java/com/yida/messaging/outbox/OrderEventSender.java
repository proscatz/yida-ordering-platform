package com.yida.messaging.outbox;

public interface OrderEventSender {
    void send(OrderOutbox outbox) throws Exception;
}