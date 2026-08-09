package com.example.restaurant.exception.types.conflict;

public class EmailAlreadyConfirmedException extends RuntimeException {
    public EmailAlreadyConfirmedException(String message) {
        super(message);
    }
}
