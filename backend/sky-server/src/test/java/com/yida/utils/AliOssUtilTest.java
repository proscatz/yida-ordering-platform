package com.yida.utils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;
import com.yida.exception.OssStorageException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AliOssUtilTest {
    private static final String OBJECT = "catalog/2026/08/22/image.png";

    @Test
    void successfulUploadSetsContentTypeAndBuildsUrlWithoutDuplicatingProtocol() {
        OSS client = mock(OSS.class);
        allowUpload(client);
        AliOssUtil util = util(client, "https://oss-cn-test.aliyuncs.com", null);
        String url = util.upload(new byte[]{1, 2, 3}, OBJECT, "image/png");
        assertEquals("https://bucket.oss-cn-test.aliyuncs.com/" + OBJECT, url);
        verify(client).shutdown();
    }

    @Test
    void customPublicBaseUrlTakesPrecedence() {
        OSS client = mock(OSS.class);
        allowUpload(client);
        AliOssUtil util = util(client, "oss-cn-test.aliyuncs.com", "https://img.example.test/");
        assertEquals("https://img.example.test/" + OBJECT,
                util.upload(new byte[]{1}, OBJECT, "image/png"));
    }

    @Test
    void accessDeniedIsClassifiedAndNeverReturnsAFakeUrl() {
        OSS client = mock(OSS.class);
        OSSException denied = mock(OSSException.class);
        when(denied.getErrorCode()).thenReturn("AccessDenied");
        when(denied.getRequestId()).thenReturn("request-safe-id");
        when(client.putObject(eq("bucket"), eq(OBJECT), any(InputStream.class), any(ObjectMetadata.class)))
                .thenThrow(denied);
        OssStorageException exception = assertThrows(OssStorageException.class,
                () -> util(client, "oss-cn-test.aliyuncs.com", null)
                        .upload(new byte[]{1}, OBJECT, "image/png"));
        assertEquals(OssStorageException.Reason.AUTHENTICATION, exception.getReason());
        assertTrue(exception.getMessage().contains("认证失败"));
        verify(client).shutdown();
    }

    @Test
    void wrappedInvalidAccessKeyResponseIsStillClassifiedAsAuthentication() {
        OSS client = mock(OSS.class);
        OSSException denied = mock(OSSException.class);
        when(denied.getErrorCode()).thenReturn("InvalidResponse");
        when(denied.getRawResponseError()).thenReturn("<Code>InvalidAccessKeyId</Code>");
        when(client.putObject(eq("bucket"), eq(OBJECT), any(InputStream.class), any(ObjectMetadata.class)))
                .thenThrow(denied);
        OssStorageException exception = assertThrows(OssStorageException.class,
                () -> util(client, "oss-cn-test.aliyuncs.com", null)
                        .upload(new byte[]{1}, OBJECT, "image/png"));
        assertEquals(OssStorageException.Reason.AUTHENTICATION, exception.getReason());
        assertTrue(exception.getMessage().contains("认证失败"));
    }

    @Test
    void networkTimeoutUsesLimitedRetriesWithTheSameObjectName() {
        OSS client = mock(OSS.class);
        when(client.putObject(eq("bucket"), eq(OBJECT), any(InputStream.class), any(ObjectMetadata.class)))
                .thenThrow(new ClientException("connection timed out"));
        OssStorageException exception = assertThrows(OssStorageException.class,
                () -> util(client, "oss-cn-test.aliyuncs.com", null)
                        .upload(new byte[]{1}, OBJECT, "image/png"));
        assertEquals(OssStorageException.Reason.TIMEOUT, exception.getReason());
        verify(client, times(3)).putObject(eq("bucket"), eq(OBJECT), any(InputStream.class), any(ObjectMetadata.class));
        verify(client, times(3)).shutdown();
    }

    @Test
    void responseAndLogsNeverContainCredentialsOrRawUpstreamMessage() {
        String credentialSentinel = "credential-sentinel-value";
        OSS client = mock(OSS.class);
        when(client.putObject(eq("bucket"), eq(OBJECT), any(InputStream.class), any(ObjectMetadata.class)))
                .thenThrow(new ClientException("upstream-private-detail " + credentialSentinel));
        Logger logger = (Logger) LoggerFactory.getLogger(AliOssUtil.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            OssStorageException exception = assertThrows(OssStorageException.class,
                    () -> new AliOssUtil("oss-cn-test.aliyuncs.com", credentialSentinel,
                            credentialSentinel, "bucket", null, () -> client)
                            .upload(new byte[]{1}, OBJECT, "image/png"));
            assertFalse(exception.getMessage().contains(credentialSentinel));
            assertTrue(appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                    .noneMatch(message -> message.contains(credentialSentinel)));
        } finally {
            logger.detachAppender(appender);
        }
    }

    private AliOssUtil util(OSS client, String endpoint, String publicBaseUrl) {
        return new AliOssUtil(endpoint, "test-id", "test-secret", "bucket", publicBaseUrl, () -> client);
    }

    private void allowUpload(OSS client) {
        when(client.putObject(eq("bucket"), eq(OBJECT), any(InputStream.class), any(ObjectMetadata.class)))
                .thenReturn(new PutObjectResult());
    }
}
