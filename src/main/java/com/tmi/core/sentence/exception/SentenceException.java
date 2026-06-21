package com.tmi.core.sentence.exception;

import com.tmi.core.common.exception.BaseException;
import com.tmi.core.common.exception.IErrorType;

public class SentenceException extends BaseException {

    public SentenceException(IErrorType errorType, Object... args) {
        super(errorType, args);
    }

    public SentenceException(IErrorType errorType, Throwable cause, Object... args) {
        super(errorType, cause, args);
    }

}
