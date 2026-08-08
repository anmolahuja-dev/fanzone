package com.fanzone.common.exceptions;

public class ValidationException extends FanzoneException {

    private final String code;

    public ValidationException(String message) {
        super(message);
        this.code = "VALIDATION_FAILED";
    }

    public ValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public int getHttpStatus() {
        return 400;
    }
}
