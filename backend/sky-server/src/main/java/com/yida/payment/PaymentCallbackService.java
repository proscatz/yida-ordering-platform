package com.yida.payment;

import com.yida.constant.MessageConstant;
import com.yida.entity.Orders;
import com.yida.exception.OrderBusinessException;
import com.yida.mapper.OrderMapper;
import com.yida.properties.WeChatProperties;
import com.yida.service.support.OrderStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentCallbackService {
    private static final String PROVIDER = "WECHAT";
    private final PaymentRecordMapper records; private final OrderMapper orders; private final WeChatProperties properties;
    public PaymentCallbackService(PaymentRecordMapper records, OrderMapper orders, WeChatProperties properties) {
        this.records=records; this.orders=orders; this.properties=properties;
    }
    @Transactional(rollbackFor = Exception.class)
    public void process(WeChatPaymentCallback callback) {
        validateEnvelope(callback);
        PaymentCallbackRecord existing=records.getCallback(PROVIDER, callback.getEventId());
        if(existing!=null){ requireSame(existing,callback); return; }
        Orders order=orders.getByNumberForUpdate(callback.getOrderNumber());
        if(order==null) throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        PaymentCallbackRecord committedWhileWaiting=records.getCallback(PROVIDER,callback.getEventId());
        if(committedWhileWaiting!=null){requireSame(committedWhileWaiting,callback);return;}
        if(toCent(order.getAmount())!=callback.getAmountCent()) throw new OrderBusinessException(MessageConstant.PAYMENT_AMOUNT_MISMATCH);
        if(!Orders.PENDING_PAYMENT.equals(order.getStatus()) || !Orders.UN_PAID.equals(order.getPayStatus()))
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        PaymentCallbackRecord record=PaymentCallbackRecord.builder().provider(PROVIDER).eventId(callback.getEventId())
                .orderNumber(callback.getOrderNumber()).transactionId(callback.getTransactionId())
                .amountCent(callback.getAmountCent()).rawHash(callback.getRawHash()).createTime(LocalDateTime.now()).build();
        if(records.insertCallback(record)==0){ requireSame(records.getCallback(PROVIDER,callback.getEventId()),callback); return; }
        OrderStateMachine.requireTransition(order.getStatus(),Orders.TO_BE_CONFIRMED);
        Orders update=Orders.builder().id(order.getId()).status(Orders.TO_BE_CONFIRMED).payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now()).build();
        if(orders.updateByIdAndStatus(update,Orders.PENDING_PAYMENT)!=1)
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
    }
    private void validateEnvelope(WeChatPaymentCallback c){
        if(c==null||!StringUtils.hasText(c.getEventId())||!StringUtils.hasText(c.getOrderNumber())
                ||!StringUtils.hasText(c.getTransactionId())||!properties.getMchid().equals(c.getMerchantId())
                ||!properties.getAppid().equals(c.getAppId())||!"SUCCESS".equals(c.getTradeState())
                ||!"CNY".equals(c.getCurrency())||c.getAmountCent()<=0) throw new SecurityException("微信支付回调业务字段校验失败");
    }
    private void requireSame(PaymentCallbackRecord e,WeChatPaymentCallback c){
        if(e==null||!e.getOrderNumber().equals(c.getOrderNumber())||!e.getTransactionId().equals(c.getTransactionId())
                ||e.getAmountCent().longValue()!=c.getAmountCent()||!e.getRawHash().equals(c.getRawHash()))
            throw new SecurityException("重复回调内容不一致");
    }
    private long toCent(BigDecimal amount){try{return amount.movePointRight(2).longValueExact();}
        catch(Exception ex){throw new OrderBusinessException(MessageConstant.ORDER_AMOUNT_ERROR);}}
}