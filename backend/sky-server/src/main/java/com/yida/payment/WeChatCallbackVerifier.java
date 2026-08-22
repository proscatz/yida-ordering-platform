package com.yida.payment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yida.properties.WeChatProperties;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Component
public class WeChatCallbackVerifier {
    private final WeChatProperties properties;

    public WeChatCallbackVerifier(WeChatProperties properties) { this.properties = properties; }

    public WeChatPaymentCallback verifyAndDecrypt(String timestamp, String nonce, String serial,
                                                   String signatureText, String body) {
        requireText(timestamp, nonce, serial, signatureText, body);
        long callbackTime;
        try { callbackTime = Long.parseLong(timestamp); }
        catch (NumberFormatException ex) { throw new SecurityException("微信支付回调时间戳无效"); }
        if (Math.abs(System.currentTimeMillis() / 1000 - callbackTime) > 300) {
            throw new SecurityException("微信支付回调已过期");
        }
        try (FileInputStream input = new FileInputStream(properties.getWeChatPayCertFilePath())) {
            X509Certificate certificate = PemUtil.loadCertificate(input);
            if (!certificate.getSerialNumber().toString(16).equalsIgnoreCase(serial)) {
                throw new SecurityException("微信平台证书序列号不匹配");
            }
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(certificate.getPublicKey());
            verifier.update((timestamp + "\n" + nonce + "\n" + body + "\n").getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getDecoder().decode(signatureText))) {
                throw new SecurityException("微信支付回调签名无效");
            }
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SecurityException("微信支付回调验签失败", ex);
        }

        JSONObject notification = JSON.parseObject(body);
        JSONObject resource = notification == null ? null : notification.getJSONObject("resource");
        if (resource == null) { throw new SecurityException("微信支付回调报文缺少 resource"); }
        try {
            AesUtil aes = new AesUtil(properties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
            String plain = aes.decryptToString(bytes(resource.getString("associated_data")),
                    bytes(resource.getString("nonce")), resource.getString("ciphertext"));
            JSONObject transaction = JSON.parseObject(plain);
            JSONObject amount = transaction.getJSONObject("amount");
            if (amount == null) { throw new SecurityException("微信支付回调金额缺失"); }
            return WeChatPaymentCallback.builder().eventId(notification.getString("id"))
                    .merchantId(transaction.getString("mchid")).appId(transaction.getString("appid"))
                    .orderNumber(transaction.getString("out_trade_no"))
                    .transactionId(transaction.getString("transaction_id"))
                    .tradeState(transaction.getString("trade_state"))
                    .amountCent(amount.getLongValue("total")).currency(amount.getString("currency"))
                    .rawHash(sha256(body)).build();
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SecurityException("微信支付回调解密失败", ex);
        }
    }

    private void requireText(String... values) {
        for (String value : values) if (!StringUtils.hasText(value)) throw new SecurityException("微信支付回调参数不完整");
    }
    private byte[] bytes(String value) { return value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8); }
    private String sha256(String value) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : hash) result.append(String.format("%02x", b));
        return result.toString();
    }
}