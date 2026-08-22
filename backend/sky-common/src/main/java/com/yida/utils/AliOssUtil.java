package com.yida.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.yida.exception.OssStorageException;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

@Slf4j
public class AliOssUtil {
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MILLIS = 150L;
    private static final Set<String> AUTH_ERROR_CODES = Set.of(
            "AccessDenied", "InvalidAccessKeyId", "SignatureDoesNotMatch", "SecurityTokenExpired");
    private static final Set<String> TRANSIENT_ERROR_CODES = Set.of(
            "RequestTimeout", "ServiceUnavailable", "InternalError", "OperationTimeout");

    private final String endpoint;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String bucketName;
    private final String publicBaseUrl;
    private final Supplier<OSS> clientFactory;

    public AliOssUtil(String endpoint, String accessKeyId, String accessKeySecret,
                      String bucketName, String publicBaseUrl) {
        this(endpoint, accessKeyId, accessKeySecret, bucketName, publicBaseUrl,
                () -> buildClient(endpoint, accessKeyId, accessKeySecret));
    }

    public AliOssUtil(String endpoint, String accessKeyId, String accessKeySecret,
                      String bucketName, String publicBaseUrl, Supplier<OSS> clientFactory) {
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.bucketName = bucketName;
        this.publicBaseUrl = publicBaseUrl;
        this.clientFactory = clientFactory;
    }

    public String upload(byte[] bytes, String objectName, String contentType) {
        validateConfiguration(objectName);
        Instant started = Instant.now();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            OSS client = null;
            try {
                client = clientFactory.get();
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(contentType);
                metadata.setContentLength(bytes.length);
                client.putObject(bucketName, objectName, new ByteArrayInputStream(bytes), metadata);
                log.info("OSS upload succeeded type={} object={} attempts={} elapsedMs={}",
                        "PUT_OBJECT", objectName, attempt, elapsedMillis(started));
                return publicUrl(objectName);
            } catch (OSSException ex) {
                OssStorageException mapped = mapOssException(ex, objectName);
                log.warn("OSS upload failed type={} code={} requestId={} object={} attempt={} elapsedMs={}",
                        mapped.getReason(), mapped.getErrorCode(), mapped.getRequestId(), objectName,
                        attempt, elapsedMillis(started));
                if (!isTransient(ex) || attempt == MAX_ATTEMPTS) throw mapped;
                backoff(attempt, objectName);
            } catch (ClientException ex) {
                OssStorageException mapped = mapClientException(ex, objectName);
                log.warn("OSS client failed type={} exception={} rootCause={} code={} requestId={} object={} attempt={} elapsedMs={}",
                        mapped.getReason(), ex.getClass().getSimpleName(), rootCauseName(ex),
                        mapped.getErrorCode(), mapped.getRequestId(), objectName, attempt, elapsedMillis(started));
                if (mapped.getReason() != OssStorageException.Reason.TIMEOUT || attempt == MAX_ATTEMPTS) {
                    throw mapped;
                }
                backoff(attempt, objectName);
            } catch (OssStorageException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                log.error("OSS upload failed type={} object={} elapsedMs={}",
                        OssStorageException.Reason.UNKNOWN, objectName, elapsedMillis(started));
                throw storageException(OssStorageException.Reason.UNKNOWN, null, null, objectName, ex);
            } finally {
                if (client != null) client.shutdown();
            }
        }
        throw storageException(OssStorageException.Reason.UNKNOWN, null, null, objectName, null);
    }

    public void checkHealth() {
        validateConfiguration("health-check");
        OSS client = null;
        try {
            client = clientFactory.get();
            if (!client.doesBucketExist(bucketName)) {
                throw storageException(OssStorageException.Reason.BUCKET_NOT_FOUND,
                        "NoSuchBucket", null, "health-check", null);
            }
        } catch (OSSException ex) {
            throw mapOssException(ex, "health-check");
        } catch (ClientException ex) {
            throw mapClientException(ex, "health-check");
        } finally {
            if (client != null) client.shutdown();
        }
    }

    private void validateConfiguration(String objectName) {
        if (isBlank(endpoint) || isBlank(accessKeyId) || isBlank(accessKeySecret) || isBlank(bucketName)) {
            throw storageException(OssStorageException.Reason.CONFIGURATION,
                    "InvalidConfiguration", null, objectName, null);
        }
    }

    private OssStorageException mapOssException(OSSException ex, String objectName) {
        String code = ex.getErrorCode();
        OssStorageException.Reason reason;
        if (isAuthenticationError(ex)) reason = OssStorageException.Reason.AUTHENTICATION;
        else if ("NoSuchBucket".equals(code)) reason = OssStorageException.Reason.BUCKET_NOT_FOUND;
        else if (isTransient(ex)) reason = "RequestTimeout".equals(code) || "OperationTimeout".equals(code)
                ? OssStorageException.Reason.TIMEOUT : OssStorageException.Reason.SERVICE_UNAVAILABLE;
        else reason = OssStorageException.Reason.SERVICE_UNAVAILABLE;
        return storageException(reason, code, ex.getRequestId(), objectName, ex);
    }

    private OssStorageException mapClientException(ClientException ex, String objectName) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
        boolean timeout = message.contains("timeout") || message.contains("timed out") || hasTimeoutCause(ex);
        return storageException(timeout ? OssStorageException.Reason.TIMEOUT : OssStorageException.Reason.SERVICE_UNAVAILABLE,
                timeout ? "ClientTimeout" : "ClientError", null, objectName, ex);
    }

    private boolean hasTimeoutCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String name = current.getClass().getSimpleName().toLowerCase(Locale.ROOT);
            if (name.contains("timeout")) return true;
            current = current.getCause();
        }
        return false;
    }

    private boolean isTransient(OSSException ex) {
        return ex.getErrorCode() != null && TRANSIENT_ERROR_CODES.contains(ex.getErrorCode());
    }

    private boolean isAuthenticationError(OSSException ex) {
        if (ex.getErrorCode() != null && AUTH_ERROR_CODES.contains(ex.getErrorCode())) return true;
        return containsAuthenticationSignal(ex.getErrorMessage())
                || containsAuthenticationSignal(ex.getRawResponseError());
    }

    private boolean containsAuthenticationSignal(String value) {
        if (value == null) return false;
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("invalidaccesskeyid")
                || normalized.contains("accessdenied")
                || normalized.contains("signaturedoesnotmatch")
                || normalized.contains("securitytokenexpired")
                || normalized.contains("access key id you provided is disabled");
    }

    private OssStorageException storageException(OssStorageException.Reason reason, String code,
                                                 String requestId, String objectName, Throwable cause) {
        return new OssStorageException(reason, safeMessage(reason), code, requestId, objectName, cause);
    }

    private String safeMessage(OssStorageException.Reason reason) {
        return switch (reason) {
            case CONFIGURATION -> "图片存储配置异常，请联系管理员";
            case AUTHENTICATION -> "图片存储认证失败，请联系管理员";
            case BUCKET_NOT_FOUND -> "图片存储空间不可用，请联系管理员";
            case TIMEOUT -> "图片存储连接超时，请稍后重试";
            case SERVICE_UNAVAILABLE, UNKNOWN -> "图片存储服务暂时不可用";
        };
    }

    private String publicUrl(String objectName) {
        if (!isBlank(publicBaseUrl)) return trimTrailingSlash(publicBaseUrl.trim()) + "/" + objectName;
        String host = endpoint.trim().replaceFirst("^https?://", "");
        return "https://" + bucketName + "." + trimTrailingSlash(host) + "/" + objectName;
    }

    private void backoff(int attempt, String objectName) {
        try {
            Thread.sleep(INITIAL_BACKOFF_MILLIS * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw storageException(OssStorageException.Reason.SERVICE_UNAVAILABLE,
                    "RetryInterrupted", null, objectName, ex);
        }
    }

    private static String normalizeClientEndpoint(String value) {
        if (isBlank(value)) return value;
        String normalized = trimTrailingSlash(value.trim());
        return normalized.matches("(?i)^https?://.*") ? normalized : "https://" + normalized;
    }

    private static OSS buildClient(String endpoint, String accessKeyId, String accessKeySecret) {
        ClientBuilderConfiguration configuration = new ClientBuilderConfiguration();
        configuration.setConnectionTimeout(5_000);
        configuration.setSocketTimeout(10_000);
        configuration.setMaxErrorRetry(0);
        return new OSSClientBuilder().build(normalizeClientEndpoint(endpoint), accessKeyId, accessKeySecret, configuration);
    }

    private static String rootCauseName(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getClass().getSimpleName();
    }

    private static String trimTrailingSlash(String value) {
        return value.replaceFirst("/+$", "");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static long elapsedMillis(Instant started) {
        return Duration.between(started, Instant.now()).toMillis();
    }
}
