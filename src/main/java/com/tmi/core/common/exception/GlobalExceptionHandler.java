package com.tmi.core.common.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    // 도메인 예외
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBase(BaseException e) {
        return respond(e.getErrorType(), e.getLogLevel(), e, e.getArgs());
    }

    // Spring 표준 예외
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse>
    handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Invalid request.");
        IErrorType errorType = ECommonErrorType.INVALID_REQUEST;
        ELogLevel.INFO.log(log, "[{}] {}", errorType.getErrorCode(), message);
        return ResponseEntity.status(errorType.getStatus())
                .body(ErrorResponse.of(errorType.getErrorCode(), message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse>
    handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(ECommonErrorType.INVALID_REQUEST, ELogLevel.INFO, e, e.getName(), e.getValue());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse>
    handleMissingParam(MissingServletRequestParameterException e) {
        return respond(ECommonErrorType.MISSING_PARAMETER, ELogLevel.INFO, e, e.getParameterName());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
        return respond(ECommonErrorType.NOT_FOUND_URL, ELogLevel.INFO, e, e.getHttpMethod(), e.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse>
    handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return respond(ECommonErrorType.METHOD_NOT_SUPPORTED, ELogLevel.INFO, e, e.getMethod());
    }

    // 모드 예외 (catch-all)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        return respond(ECommonErrorType.INTERNAL_ERROR, ELogLevel.ERROR, e);
    }

    // 공통 헬퍼
    private ResponseEntity<ErrorResponse> respond(IErrorType errorType, ELogLevel logLevel, Exception e, Object... args) {
        String code = errorType.getErrorCode();
        String message = messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale());
        logLevel.log(log, "[{}] {} - {}", code, message, e.getMessage());
        return ResponseEntity.status(errorType.getStatus()).body(ErrorResponse.of(code, message));
    }

}
