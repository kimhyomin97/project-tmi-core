package com.tmi.core.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ECommonErrorType implements IErrorType {

    INVALID_REQUEST  ("TMI.COMMON.INVALID_REQUEST"),
    INTERNAL_ERROR   ("TMI.COMMON.INTERNAL_ERROR"),
    NOT_FOUND_URL    ("TMI.COMMON.NOT_FOUND_URL"),
    METHOD_NOT_SUPPORTED("TMI.COMMON.METHOD_NOT_SUPPORTED"),
    MISSING_PARAMETER("TMI.COMMON.MISSING_PARAMETER"),
    BAD_REQUEST      ("TMI.COMMON.BAD_REQUEST"),
    ;

    private final String errorCode;
}
