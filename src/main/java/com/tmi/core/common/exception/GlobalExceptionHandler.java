package com.tmi.core.common.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBase(BaseException e) {
        String code = e.getErrorType().getErrorCode();
        String message = messageSource.getMessage(code, e.getArgs(), code,
                LocaleContextHolder.getLocale());
        log.warn("[{}] {}", code, message);
        return
                ResponseEntity.status(e.getStatus()).body(ErrorResponse.of(code, message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse>
    handleValidation(MethodArgumentNotValidException e) {
        String code = ECommonErrorType.INVALID_REQUEST.getErrorCode();
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Invalid request.");
        log.warn("[{}] {}", code, message);
        return
                ResponseEntity.status(ECommonErrorType.INVALID_REQUEST.getStatus())
                        .body(ErrorResponse.of(code, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        String code = ECommonErrorType.INTERNAL_ERROR.getErrorCode();
        String message = messageSource.getMessage(code, null, code,
                LocaleContextHolder.getLocale());
        log.error("[{}] unexpected error", code, e);
        return
                ResponseEntity.status(ECommonErrorType.INTERNAL_ERROR.getStatus())
                        .body(ErrorResponse.of(code, message));
    }
}
