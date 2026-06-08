package com.tmi.core.common.exception;

import org.springframework.http.HttpStatus;

public interface IErrorType {

    String getErrorCode();

    HttpStatus getStatus();
}
