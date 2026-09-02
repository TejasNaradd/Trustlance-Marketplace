package com.trustlance.disputeservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest req) {
        return new ResponseEntity<>(ErrorResponse.builder()
                .timestamp(Instant.now()).status(ex.getStatus().value())
                .code(ex.getCode()).message(ex.getMessage())
                .path(req.getRequestURI()).correlationId(req.getHeader("X-Correlation-Id"))
                .build(), ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return new ResponseEntity<>(ErrorResponse.builder()
                .timestamp(Instant.now()).status(400).code("VALIDATION_FAILED")
                .message(msg).path(req.getRequestURI()).correlationId(req.getHeader("X-Correlation-Id"))
                .build(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        return new ResponseEntity<>(ErrorResponse.builder()
                .timestamp(Instant.now()).status(500).code("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .path(req.getRequestURI()).correlationId(req.getHeader("X-Correlation-Id"))
                .build(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
