package com.yida.payment;
import com.yida.entity.Orders;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
class PaymentRefundServiceTest {
    @Test void refundUsesLocalOrderAmountAndStableIdempotencyNumber(){
        PaymentRecordMapper records=mock(PaymentRecordMapper.class);PaymentGateway gateway=mock(PaymentGateway.class);
        when(gateway.provider()).thenReturn("MOCK");when(records.insertRefund("MOCK","order-1","refund-order-1",1234)).thenReturn(1);
        new PaymentRefundService(records,gateway).refundLocalOrder(Orders.builder().number("order-1").amount(new BigDecimal("12.34")).build());
        verify(gateway).refund("order-1","refund-order-1",new BigDecimal("12.34"),new BigDecimal("12.34"));
    }
    @Test void duplicateRefundDoesNotCallGateway(){
        PaymentRecordMapper records=mock(PaymentRecordMapper.class);PaymentGateway gateway=mock(PaymentGateway.class);when(gateway.provider()).thenReturn("MOCK");
        new PaymentRefundService(records,gateway).refundLocalOrder(Orders.builder().number("order-1").amount(BigDecimal.ONE).build());
        verify(gateway,never()).refund(anyString(),anyString(),any(),any());
    }
}