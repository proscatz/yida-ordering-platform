package com.yida.exception;

public class ImageValidationException extends RuntimeException {
    public enum Reason {
        EMPTY,
        TOO_LARGE,
        UNSUPPORTED_FORMAT,
        CONTENT_MISMATCH,
        READ_FAILED
    }

    private final Reason reason;

    public ImageValidationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public ImageValidationException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
