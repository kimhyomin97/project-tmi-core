package com.tmi.core.sentence.exception;

import com.tmi.core.common.exception.IErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ESentenceErrorType implements IErrorType {

    NOT_FOUND(HttpStatus.NOT_FOUND, "TMI.SENTENCE.NOT_FOUND"),
    ;

    private final HttpStatus status;
    private final String errorCode;
}
