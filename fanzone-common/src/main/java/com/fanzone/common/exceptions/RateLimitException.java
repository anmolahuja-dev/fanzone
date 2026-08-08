package com.fanzone.common.exceptions;

public class RateLimitException extends FanzoneException {

    private final String code;

    public RateLimitException(String message) {
        super(message);
        this.code = "RATE_LIMITED";
    }

    public RateLimitException(String code, String message) {
        super(message);
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public int getHttpStatus() {
        return 429;
    }
}
