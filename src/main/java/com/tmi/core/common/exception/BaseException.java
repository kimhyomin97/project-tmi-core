package com.tmi.core.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseException extends RuntimeException {

    private final IErrorType errorType;
    private final Object[] args;

    protected BaseException(IErrorType errorType, Object... args) {
        super(errorType.getErrorCode());
        this.errorType = errorType;
        this.args = args;
    }

    public HttpStatus getStatus() {
        return errorType.getStatus();
    }
}
