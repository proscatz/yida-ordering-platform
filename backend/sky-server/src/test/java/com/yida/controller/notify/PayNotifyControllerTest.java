package com.yida.controller.notify;
import com.yida.payment.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
class PayNotifyControllerTest {
    @Test void invalidSignatureIsRejectedBeforePersistence(){
        WeChatCallbackVerifier verifier=mock(WeChatCallbackVerifier.class);PaymentCallbackService service=mock(PaymentCallbackService.class);
        when(verifier.verifyAndDecrypt(anyString(),anyString(),anyString(),anyString(),anyString())).thenThrow(new SecurityException("bad"));
        ResponseEntity<?> response=new PayNotifyController(verifier,service).paySuccessNotify("1","n","s","sig","{}");
        assertEquals(401,response.getStatusCodeValue());verifyNoInteractions(service);
    }
}