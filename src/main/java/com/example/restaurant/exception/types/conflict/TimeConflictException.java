package com.example.restaurant.exception.types.conflict;

public class TimeConflictException extends RuntimeException {
    public TimeConflictException(String message) {
        super(message);
    }
}
