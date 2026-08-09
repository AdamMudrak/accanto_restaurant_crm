package com.example.restaurant.exception.types.forbidden;

public class SelfModificationNotAllowedException extends RuntimeException {
    public SelfModificationNotAllowedException(String message) {
        super(message);
    }
}
