package com.example.restaurant.exception.handlers;

import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.exception.types.forbidden.AccountNotActivatedException;
import com.example.restaurant.exception.types.forbidden.DeletedEmployeeException;
import com.example.restaurant.exception.types.forbidden.SelfModificationNotAllowedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ForbiddenExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error("Access denied"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DeletedEmployeeException.class)
    public ResponseEntity<ApiResponse<Object>> handleDeletedEmployeeException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(SelfModificationNotAllowedException.class)
    public ResponseEntity<ApiResponse<Object>> handleSelfModificationNotAllowedException(
            Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AccountNotActivatedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountNotActivatedException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.FORBIDDEN);
    }
}
