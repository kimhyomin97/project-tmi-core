package com.tmi.core.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ECommonErrorType implements IErrorType {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "TMI.COMMON.INVALID_REQUEST"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "TMI.COMMON.INTERNAL_ERROR"),
    ;

    private final HttpStatus status;
    private final String errorCode;
}
