package com.tmi.core.common.exception;

public record ErrorResponse(Error error) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new Error(code, message));
    }

    public record Error(String code, String message) {

    }
}
