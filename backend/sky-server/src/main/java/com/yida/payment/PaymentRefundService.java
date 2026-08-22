package com.yida.payment;
import com.yida.constant.MessageConstant;
import com.yida.entity.Orders;
import com.yida.exception.OrderBusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class PaymentRefundService {
    private final PaymentRecordMapper records; private final PaymentGateway gateway;
    public PaymentRefundService(PaymentRecordMapper records,PaymentGateway gateway){this.records=records;this.gateway=gateway;}
    @Transactional(rollbackFor=Exception.class)
    public void refundLocalOrder(Orders order){
        if(order.getAmount()==null||order.getAmount().signum()<=0) throw new OrderBusinessException(MessageConstant.ORDER_AMOUNT_ERROR);
        long cents; try{cents=order.getAmount().movePointRight(2).longValueExact();}
        catch(Exception ex){throw new OrderBusinessException(MessageConstant.ORDER_AMOUNT_ERROR);}
        String refundNumber="refund-"+order.getNumber();
        if(records.insertRefund(gateway.provider(),order.getNumber(),refundNumber,cents)==0)return;
        gateway.refund(order.getNumber(),refundNumber,order.getAmount(),order.getAmount());
    }
}