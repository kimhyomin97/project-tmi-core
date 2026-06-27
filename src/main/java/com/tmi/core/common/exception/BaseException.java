package com.tmi.core.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseException extends RuntimeException {

    private static final Object[] EMPTY_ARGS = new Object[0];

    private final HttpStatus httpStatus;
    private final IErrorType errorType;
    private final Object[] args;

    private ELogLevel logLevel = ELogLevel.ERROR;
    private String logMessage;

    protected BaseException(HttpStatus httpStatus, IErrorType errorType) {
        super(errorType.getErrorCode());
        this.httpStatus = httpStatus;
        this.errorType = errorType;
        this.args = EMPTY_ARGS;
    }

    protected BaseException(HttpStatus httpStatus, IErrorType errorType, Throwable cause) {
        super(errorType.getErrorCode(), cause);
        this.httpStatus = httpStatus;
        this.errorType = errorType;
        this.args = EMPTY_ARGS;
    }

    protected BaseException(HttpStatus httpStatus, IErrorType errorType, Object... args) {
        super(errorType.getErrorCode());
        this.httpStatus = httpStatus;
        this.errorType = errorType;
        this.args = args;
    }

    protected BaseException(HttpStatus httpStatus, IErrorType errorType, Throwable cause, Object... args) {
        super(errorType.getErrorCode(), cause);
        this.httpStatus = httpStatus;
        this.errorType = errorType;
        this.args = args;
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseException> T logLevel(ELogLevel logLevel) {
        this.logLevel = logLevel;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseException> T logMessage(String logMessage) {
        this.logMessage = logMessage;
        return (T) this;
    }
}
