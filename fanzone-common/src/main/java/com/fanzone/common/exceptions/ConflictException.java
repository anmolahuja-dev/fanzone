package com.fanzone.common.exceptions;

public class ConflictException extends FanzoneException {

    private final String code;

    public ConflictException(String message) {
        super(message);
        this.code = "CONFLICT";
    }

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public int getHttpStatus() {
        return 409;
    }
}
