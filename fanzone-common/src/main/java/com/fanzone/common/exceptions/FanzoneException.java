package com.fanzone.common.exceptions;

public abstract class FanzoneException extends RuntimeException {

    protected FanzoneException(String message) {
        super(message);
    }

    protected FanzoneException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract String getCode();

    public abstract int getHttpStatus();
}
