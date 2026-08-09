package com.example.restaurant.exception.handlers;

import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.exception.types.conflict.EmailAlreadyConfirmedException;
import com.example.restaurant.exception.types.conflict.EmployeeEmailAlreadyExistsException;
import com.example.restaurant.exception.types.conflict.GuestIllegalStateException;
import com.example.restaurant.exception.types.conflict.ItemIllegalStateException;
import com.example.restaurant.exception.types.conflict.MenuCategoryIsUsedException;
import com.example.restaurant.exception.types.conflict.NoPendingEmailException;
import com.example.restaurant.exception.types.conflict.OrderIllegalStateException;
import com.example.restaurant.exception.types.conflict.PendingEmailAlreadyExistsException;
import com.example.restaurant.exception.types.conflict.TimeConflictException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConflictExceptionHandler {

    @ExceptionHandler(TimeConflictException.class)
    public ResponseEntity<ApiResponse<Object>> handleTimeConflictException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EmployeeEmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmployeeEmailAlreadyExistsException(
            Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PendingEmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handlePendingEmailAlreadyExistsException(
            Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EmailAlreadyConfirmedException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmailAlreadyConfirmedException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(NoPendingEmailException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoPendingEmailException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MenuCategoryIsUsedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMenuCategoryIsUsedException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ItemIllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleItemIllegalStateException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(GuestIllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleGuestIllegalStateException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(OrderIllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleOrderIllegalStateException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.CONFLICT);
    }
}
