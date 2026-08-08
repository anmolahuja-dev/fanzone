package com.fanzone.common.exceptions;

public class ForbiddenException extends FanzoneException {

    private final String code;

    public ForbiddenException(String message) {
        super(message);
        this.code = "FORBIDDEN";
    }

    public ForbiddenException(String code, String message) {
        super(message);
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public int getHttpStatus() {
        return 403;
    }
}
