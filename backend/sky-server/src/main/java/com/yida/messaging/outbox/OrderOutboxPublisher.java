package com.yida.messaging.outbox;

import com.yida.mapper.OrderOutboxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@ConditionalOnProperty(name = "yida.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class OrderOutboxPublisher {
    private static final int BATCH_SIZE = 50;
    private static final int MAX_ERROR_LENGTH = 500;

    private final OrderOutboxMapper outboxMapper;
    private final OrderEventSender eventSender;

    public OrderOutboxPublisher(OrderOutboxMapper outboxMapper, OrderEventSender eventSender) {
        this.outboxMapper = outboxMapper;
        this.eventSender = eventSender;
    }

    @Scheduled(fixedDelayString = "${yida.messaging.outbox-poll-millis:2000}")
    public void publishReady() {
        LocalDateTime now = LocalDateTime.now();
        outboxMapper.recoverStale(now.minusMinutes(1), now);
        List<OrderOutbox> records = outboxMapper.findReady(now, BATCH_SIZE);
        if (records == null) records = Collections.emptyList();
        for (OrderOutbox record : records) publishOne(record);
    }

    void publishOne(OrderOutbox record) {
        LocalDateTime now = LocalDateTime.now();
        if (outboxMapper.markSending(record.getId(), now) != 1) return;
        try {
            eventSender.send(record);
            if (outboxMapper.markSent(record.getId(), LocalDateTime.now()) != 1) {
                log.warn("Outbox 已发送但状态确认失败，将由补偿任务核对，eventId={}", record.getEventId());
            }
        } catch (Exception exception) {
            int retry = record.getRetryCount() == null ? 0 : record.getRetryCount();
            long backoffSeconds = Math.min(300L, 1L << Math.min(retry, 8));
            outboxMapper.markFailed(record.getId(), LocalDateTime.now().plusSeconds(backoffSeconds),
                    LocalDateTime.now(), abbreviate(exception.getMessage()));
            log.warn("订单 Outbox 发送失败，等待重试，eventId={}", record.getEventId());
        }
    }

    private String abbreviate(String message) {
        if (message == null) return "unknown";
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}