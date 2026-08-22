package com.yida.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeChatPaymentCallback {
    private String eventId;
    private String merchantId;
    private String appId;
    private String orderNumber;
    private String transactionId;
    private String tradeState;
    private long amountCent;
    private String currency;
    private String rawHash;
}