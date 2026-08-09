package com.example.restaurant.exception.types.notfound;

public class MenuCategoryNotFoundException extends RuntimeException {
    public MenuCategoryNotFoundException(String message) {
        super(message);
    }
}
