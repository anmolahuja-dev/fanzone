package com.fanzone.common.exceptions;

public class AuthenticationException extends FanzoneException {

    private final String code;

    public AuthenticationException(String message) {
        super(message);
        this.code = "AUTH_INVALID_CREDENTIALS";
    }

    public AuthenticationException(String code, String message) {
        super(message);
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public int getHttpStatus() {
        return 401;
    }
}
