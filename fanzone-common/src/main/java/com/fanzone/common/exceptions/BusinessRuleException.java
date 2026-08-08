package com.fanzone.common.exceptions;

public class BusinessRuleException extends FanzoneException {

    private final String code;

    public BusinessRuleException(String message) {
        super(message);
        this.code = "BUSINESS_RULE_VIOLATION";
    }

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public int getHttpStatus() {
        return 422;
    }
}
