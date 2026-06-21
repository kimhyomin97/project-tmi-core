package com.tmi.core.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ECommonErrorType implements IErrorType {

    INVALID_REQUEST  (HttpStatus.BAD_REQUEST, "TMI.COMMON.INVALID_REQUEST"),
    INTERNAL_ERROR   (HttpStatus.INTERNAL_SERVER_ERROR, "TMI.COMMON.INTERNAL_ERROR"),
    NOT_FOUND_URL    (HttpStatus.NOT_FOUND, "TMI.COMMON.NOT_FOUND_URL"),
    METHOD_NOT_SUPPORTED(HttpStatus.METHOD_NOT_ALLOWED, "TMI.COMMON.METHOD_NOT_SUPPORTED"),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "TMI.COMMON.MISSING_PARAMETER"),
    BAD_REQUEST      (HttpStatus.BAD_REQUEST, "TMI.COMMON.BAD_REQUEST"),
    ;

    private final HttpStatus status;
    private final String errorCode;
}
