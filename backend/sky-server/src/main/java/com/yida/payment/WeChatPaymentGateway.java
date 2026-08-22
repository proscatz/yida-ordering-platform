package com.yida.payment;

import com.alibaba.fastjson.JSONObject;
import com.yida.utils.WeChatPayUtil;
import com.yida.vo.OrderPaymentVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "yida.payment.provider", havingValue = "wechat")
public class WeChatPaymentGateway implements PaymentGateway {
    private final WeChatPayUtil weChatPayUtil;

    public WeChatPaymentGateway(WeChatPayUtil weChatPayUtil) { this.weChatPayUtil = weChatPayUtil; }

    @Override
    public OrderPaymentVO createPayment(PaymentRequest request) {
        try {
            JSONObject result = weChatPayUtil.pay(request.getOrderNumber(), request.getAmount(),
                    request.getDescription(), request.getOpenid());
            return OrderPaymentVO.builder().nonceStr(result.getString("nonceStr"))
                    .paySign(result.getString("paySign")).timeStamp(result.getString("timeStamp"))
                    .signType(result.getString("signType")).packageStr(result.getString("package")).build();
        } catch (Exception ex) {
            throw new IllegalStateException("微信支付下单失败", ex);
        }
    }

    @Override
    public void refund(String orderNumber, String refundNumber,
                       java.math.BigDecimal refundAmount, java.math.BigDecimal totalAmount) {
        try {
            weChatPayUtil.refund(orderNumber, refundNumber, refundAmount, totalAmount);
        } catch (Exception ex) {
            throw new IllegalStateException("微信退款失败", ex);
        }
    }

    @Override public String provider() { return "WECHAT"; }
}