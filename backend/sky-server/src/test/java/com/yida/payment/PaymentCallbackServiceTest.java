package com.yida.payment;

import com.yida.entity.Orders;
import com.yida.exception.OrderBusinessException;
import com.yida.mapper.OrderMapper;
import com.yida.properties.WeChatProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentCallbackServiceTest {
    private PaymentRecordMapper records;private OrderMapper orders;private PaymentCallbackService service;
    @BeforeEach void setUp(){records=mock(PaymentRecordMapper.class);orders=mock(OrderMapper.class);WeChatProperties p=new WeChatProperties();p.setMchid("merchant-1");p.setAppid("app-1");service=new PaymentCallbackService(records,orders,p);}
    @Test void validCallbackUsesLocalAmountAndTransitionsOnce(){
        WeChatPaymentCallback callback=callback();when(orders.getByNumberForUpdate("order-1")).thenReturn(order());
        when(records.insertCallback(any())).thenReturn(1);when(orders.updateByIdAndStatus(any(),eq(Orders.PENDING_PAYMENT))).thenReturn(1);
        service.process(callback);verify(records).insertCallback(argThat(r->r.getAmountCent()==1234));verify(orders).updateByIdAndStatus(any(),eq(Orders.PENDING_PAYMENT));
    }
    @Test void exactRepeatedCallbackIsIdempotent(){
        WeChatPaymentCallback callback=callback();when(records.getCallback("WECHAT","event-1")).thenReturn(record());
        service.process(callback);verify(orders,never()).getByNumberForUpdate(anyString());verify(records,never()).insertCallback(any());
    }
    @Test void callbackCommittedWhileWaitingForOrderLockIsIdempotent(){
        WeChatPaymentCallback callback=callback();when(records.getCallback("WECHAT","event-1")).thenReturn(null,record());
        when(orders.getByNumberForUpdate("order-1")).thenReturn(order());service.process(callback);
        verify(records,never()).insertCallback(any());verify(orders,never()).updateByIdAndStatus(any(),anyInt());
    }
    @Test void alteredRepeatedCallbackIsRejected(){
        WeChatPaymentCallback callback=callback();callback.setAmountCent(999);when(records.getCallback("WECHAT","event-1")).thenReturn(record());
        assertThrows(SecurityException.class,()->service.process(callback));
    }
    @Test void callbackAmountMustEqualLocalOrder(){
        WeChatPaymentCallback callback=callback();callback.setAmountCent(1200);when(orders.getByNumberForUpdate("order-1")).thenReturn(order());
        assertThrows(OrderBusinessException.class,()->service.process(callback));verify(records,never()).insertCallback(any());
    }
    private Orders order(){return Orders.builder().id(1L).number("order-1").amount(new BigDecimal("12.34")).status(Orders.PENDING_PAYMENT).payStatus(Orders.UN_PAID).build();}
    private WeChatPaymentCallback callback(){return WeChatPaymentCallback.builder().eventId("event-1").merchantId("merchant-1").appId("app-1").orderNumber("order-1").transactionId("tx-1").tradeState("SUCCESS").amountCent(1234).currency("CNY").rawHash("hash-1").build();}
    private PaymentCallbackRecord record(){return PaymentCallbackRecord.builder().provider("WECHAT").eventId("event-1").orderNumber("order-1").transactionId("tx-1").amountCent(1234L).rawHash("hash-1").build();}
}