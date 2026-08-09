package com.example.restaurant.exception.types.badrequest;

public class InvalidImageException extends RuntimeException {
    public InvalidImageException(String message) {
        super(message);
    }
}
