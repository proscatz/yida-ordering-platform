package com.yida.handler;

import com.yida.exception.ImageValidationException;
import com.yida.exception.OssStorageException;
import com.yida.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerUploadTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void maxUploadSizeUses413AndSafeMessage() {
        ResponseEntity<Result<Object>> response = handler.maxUploadSizeHandler(
                new MaxUploadSizeExceededException(5L * 1024 * 1024));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals("图片超过 5MB", response.getBody().getMsg());
    }

    @Test
    void malformedMultipartUses400() {
        ResponseEntity<Result<Object>> response = handler.multipartHandler(new MultipartException("private-detail"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("图片上传请求格式不正确", response.getBody().getMsg());
    }

    @Test
    void imageValidationUses400AndSpecificMessage() {
        ResponseEntity<Result<Object>> response = handler.imageValidationHandler(
                new ImageValidationException(ImageValidationException.Reason.CONTENT_MISMATCH,
                        "图片内容与扩展名不一致"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("图片内容与扩展名不一致", response.getBody().getMsg());
    }

    @Test
    void ossAuthenticationUses503WithoutExposingCause() {
        String sensitiveSentinel = "sensitive-sentinel";
        OssStorageException exception = new OssStorageException(
                OssStorageException.Reason.AUTHENTICATION,
                "图片存储认证失败，请联系管理员", "AccessDenied", "request-id", "catalog/test.png",
                new RuntimeException(sensitiveSentinel));
        ResponseEntity<Result<Object>> response = handler.ossStorageHandler(exception);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("图片存储认证失败，请联系管理员", response.getBody().getMsg());
        assertFalse(response.getBody().getMsg().contains(sensitiveSentinel));
    }
}
