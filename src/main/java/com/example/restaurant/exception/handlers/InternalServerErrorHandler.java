package com.example.restaurant.exception.handlers;

import com.example.restaurant.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class InternalServerErrorHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpectedException(Exception ex) {
        log.error("Unexpected error", ex);

        return new ResponseEntity<>(
                ApiResponse.error("Internal server error"),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
