package com.example.restaurant.exception.handlers;

import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.exception.types.gone.ExpiredTokenException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GoneExceptionHandler {

    @ExceptionHandler(ExpiredTokenException.class)
    public ResponseEntity<ApiResponse<Object>> handleExpiredTokenException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.GONE);
    }
}
