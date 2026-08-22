package com.yida.payment;

import com.yida.vo.OrderPaymentVO;

import java.math.BigDecimal;

public interface PaymentGateway {
    OrderPaymentVO createPayment(PaymentRequest request);
    void refund(String orderNumber, String refundNumber, BigDecimal refundAmount, BigDecimal totalAmount);
    String provider();
    default boolean completesSynchronously() { return false; }
}