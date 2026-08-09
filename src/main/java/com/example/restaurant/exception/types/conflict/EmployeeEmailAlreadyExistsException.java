package com.example.restaurant.exception.types.conflict;

public class EmployeeEmailAlreadyExistsException extends RuntimeException {
    public EmployeeEmailAlreadyExistsException(String message) {
        super(message);
    }
}
