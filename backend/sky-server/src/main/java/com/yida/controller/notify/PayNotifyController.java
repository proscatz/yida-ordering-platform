package com.yida.controller.notify;
import com.yida.payment.PaymentCallbackService;
import com.yida.payment.WeChatCallbackVerifier;
import com.yida.payment.WeChatPaymentCallback;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.Map;
@RestController
@RequestMapping("/notify")
public class PayNotifyController {
    private static final int MAX_CALLBACK_SIZE=1024*1024;
    private final WeChatCallbackVerifier verifier; private final PaymentCallbackService callbackService;
    public PayNotifyController(WeChatCallbackVerifier verifier,PaymentCallbackService callbackService){this.verifier=verifier;this.callbackService=callbackService;}
    @PostMapping("/paySuccess")
    public ResponseEntity<Map<String,String>> paySuccessNotify(@RequestHeader("Wechatpay-Timestamp") String timestamp,
            @RequestHeader("Wechatpay-Nonce") String nonce,@RequestHeader("Wechatpay-Serial") String serial,
            @RequestHeader("Wechatpay-Signature") String signature,@RequestBody String body){
        if(body==null||body.isEmpty()||body.length()>MAX_CALLBACK_SIZE)return failure(400,"INVALID_BODY");
        try{WeChatPaymentCallback callback=verifier.verifyAndDecrypt(timestamp,nonce,serial,signature,body);
            callbackService.process(callback);return ResponseEntity.ok(Collections.singletonMap("code","SUCCESS"));}
        catch(SecurityException ex){return failure(401,"INVALID_SIGNATURE_OR_PAYLOAD");}
        catch(RuntimeException ex){return failure(400,"INVALID_TRANSACTION");}
    }
    private ResponseEntity<Map<String,String>> failure(int status,String code){return ResponseEntity.status(status).body(Collections.singletonMap("code",code));}
}