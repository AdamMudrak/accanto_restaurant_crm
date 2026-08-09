package com.example.restaurant.exception.types.notfound;

public class WorkingDayNotFoundException extends RuntimeException {
    public WorkingDayNotFoundException(String message) {
        super(message);
    }
}
