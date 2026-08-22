package com.yida.exception;

public class AddressValidationException extends BaseException {
    private final String field;

    public AddressValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
