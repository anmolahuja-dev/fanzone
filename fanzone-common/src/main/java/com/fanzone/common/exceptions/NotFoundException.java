package com.fanzone.common.exceptions;

public class NotFoundException extends FanzoneException {

    private final String code;

    public NotFoundException(String message) {
        super(message);
        this.code = "NOT_FOUND";
    }

    public NotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public int getHttpStatus() {
        return 404;
    }
}
