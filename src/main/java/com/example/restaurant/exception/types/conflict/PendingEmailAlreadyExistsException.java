package com.example.restaurant.exception.types.conflict;

public class PendingEmailAlreadyExistsException extends RuntimeException {
    public PendingEmailAlreadyExistsException(String message) {
        super(message);
    }
}
