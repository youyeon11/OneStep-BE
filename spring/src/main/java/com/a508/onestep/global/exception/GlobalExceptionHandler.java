package com.a508.onestep.global.exception;

import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.response.ErrorResponse;
import com.a508.onestep.global.logging.utils.LogUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest request) {
        LogUtils.error("Business Exception: {}", request);
        LogUtils.exceptionWithCause(e);
        return ErrorResponse.of(e.getBaseCode());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        LogUtils.error("Resource Not Found", request);
        String path = request.getRequestURI();

        // Swagger 관련 경로는 Spring이 기본 처리하도록 예외를 다시 던짐
        if (path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/swagger-resources")) {
            throw e;
        }

        LogUtils.exceptionWithCause(e);
        return ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        LogUtils.warn(
                "Invalid HTTP Method: {} {} (supported: {})",
                request.getMethod(),
                request.getRequestURI(),
                e.getSupportedHttpMethods()
        );

        return ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        LogUtils.error("Unhandled exception", request);
        LogUtils.exceptionWithCause(e);
        return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}