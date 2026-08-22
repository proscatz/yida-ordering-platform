package com.yida.payment;

import com.yida.vo.OrderPaymentVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "yida.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {
    @Override
    public OrderPaymentVO createPayment(PaymentRequest request) {
        requirePositive(request.getAmount());
        return OrderPaymentVO.builder().nonceStr(UUID.randomUUID().toString().replace("-", ""))
                .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                .signType("MOCK").packageStr("mock_order=" + request.getOrderNumber())
                .paySign("MOCK").build();
    }

    @Override
    public void refund(String orderNumber, String refundNumber, BigDecimal refundAmount, BigDecimal totalAmount) {
        requirePositive(refundAmount);
        if (refundAmount.compareTo(totalAmount) > 0) {
            throw new IllegalArgumentException("refund exceeds local order amount");
        }
    }

    @Override public String provider() { return "MOCK"; }
    @Override public boolean completesSynchronously() { return true; }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("payment amount must be positive");
        }
    }
}