package com.example.restaurant.exception.handlers;

import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.exception.types.notfound.DiningTableNotFoundException;
import com.example.restaurant.exception.types.notfound.EmployeeShiftNotFoundException;
import com.example.restaurant.exception.types.notfound.GuestNotFoundException;
import com.example.restaurant.exception.types.notfound.ImageNotFoundException;
import com.example.restaurant.exception.types.notfound.MenuCategoryNotFoundException;
import com.example.restaurant.exception.types.notfound.MenuItemNotFoundException;
import com.example.restaurant.exception.types.notfound.OrderItemNotFoundException;
import com.example.restaurant.exception.types.notfound.OrderNotFoundException;
import com.example.restaurant.exception.types.notfound.RestaurantNotFoundException;
import com.example.restaurant.exception.types.notfound.WorkingDayNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NotFoundExceptionHandler {

    @ExceptionHandler(RestaurantNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleRestaurantNotFoundException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(WorkingDayNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleWorkingDayNotFoundException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleImageNotFoundException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MenuCategoryNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleMenuCategoryNotFoundException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleEntityNotFoundException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MenuItemNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleMenuItemNotFoundException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DiningTableNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleDiningTableNotFound(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(GuestNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleGuestNotFoundException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleOrderNotFoundException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OrderItemNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleOrderItemNotFoundException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmployeeShiftNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmployeeShiftNotFoundException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }
}
