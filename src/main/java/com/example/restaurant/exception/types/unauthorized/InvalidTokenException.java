package com.example.restaurant.exception.types.unauthorized;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
