package com.example.restaurant.exception.types.serviceunavailable;

public class EmailException extends RuntimeException {
    public EmailException(String message, Exception cause) {
        super(message, cause);
    }
}
