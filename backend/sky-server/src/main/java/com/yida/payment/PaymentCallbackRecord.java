package com.yida.payment;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder
public class PaymentCallbackRecord {
    private Long id; private String provider; private String eventId; private String orderNumber;
    private String transactionId; private Long amountCent; private String rawHash; private LocalDateTime createTime;
}