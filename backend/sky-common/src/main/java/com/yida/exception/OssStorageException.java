package com.yida.exception;

public class OssStorageException extends RuntimeException {
    public enum Reason {
        CONFIGURATION,
        AUTHENTICATION,
        BUCKET_NOT_FOUND,
        TIMEOUT,
        SERVICE_UNAVAILABLE,
        UNKNOWN
    }

    private final Reason reason;
    private final String errorCode;
    private final String requestId;
    private final String objectName;

    public OssStorageException(Reason reason, String safeMessage, String errorCode,
                               String requestId, String objectName, Throwable cause) {
        super(safeMessage, cause);
        this.reason = reason;
        this.errorCode = errorCode;
        this.requestId = requestId;
        this.objectName = objectName;
    }

    public Reason getReason() {
        return reason;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getObjectName() {
        return objectName;
    }
}
