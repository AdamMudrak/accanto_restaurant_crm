package com.example.restaurant.exception.types.notfound;

public class DiningTableNotFoundException extends RuntimeException {
    public DiningTableNotFoundException(String message) {
        super(message);
    }
}
