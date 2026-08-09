package com.example.restaurant.exception.types.forbidden;

public class DeletedEmployeeException extends RuntimeException {
    public DeletedEmployeeException(String message) {
        super(message);
    }
}
