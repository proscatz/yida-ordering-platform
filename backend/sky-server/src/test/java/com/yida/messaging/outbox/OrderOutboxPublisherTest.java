package com.yida.messaging.outbox;

import com.yida.mapper.OrderOutboxMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderOutboxPublisherTest {
    @Mock
    private OrderOutboxMapper outboxMapper;
    @Mock
    private OrderEventSender eventSender;
    private OrderOutboxPublisher publisher;
    private OrderOutbox record;

    @BeforeEach
    void setUp() {
        publisher = new OrderOutboxPublisher(outboxMapper, eventSender);
        record = OrderOutbox.builder().id(1L).eventId("event-1")
                .status(OrderOutbox.PENDING).retryCount(0).build();
    }

    @Test
    void confirmedSendMarksOutboxSent() throws Exception {
        when(outboxMapper.markSending(eq(1L), any())).thenReturn(1);
        when(outboxMapper.markSent(eq(1L), any())).thenReturn(1);

        publisher.publishOne(record);

        verify(eventSender).send(record);
        verify(outboxMapper).markSent(eq(1L), any());
        verify(outboxMapper, never()).markFailed(any(), any(), any(), any());
    }

    @Test
    void failedSendReturnsRecordToRetryWithBackoff() throws Exception {
        when(outboxMapper.markSending(eq(1L), any())).thenReturn(1);
        doThrow(new IllegalStateException("broker unavailable")).when(eventSender).send(record);

        publisher.publishOne(record);

        verify(outboxMapper).markFailed(eq(1L), any(LocalDateTime.class),
                any(LocalDateTime.class), eq("broker unavailable"));
        verify(outboxMapper, never()).markSent(any(), any());
    }

    @Test
    void compensationPollRecoversStaleSendingRecords() {
        when(outboxMapper.findReady(any(), eq(50))).thenReturn(Collections.emptyList());

        publisher.publishReady();

        verify(outboxMapper).recoverStale(any(), any());
        verify(outboxMapper).findReady(any(), eq(50));
    }

    @Test
    void anotherPublisherWinningClaimPreventsDuplicateSend() throws Exception {
        when(outboxMapper.markSending(eq(1L), any())).thenReturn(0);

        publisher.publishOne(record);

        verify(eventSender, never()).send(any());
    }
}