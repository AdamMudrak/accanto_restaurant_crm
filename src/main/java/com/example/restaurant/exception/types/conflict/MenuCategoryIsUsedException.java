package com.example.restaurant.exception.types.conflict;

public class MenuCategoryIsUsedException extends RuntimeException {
    public MenuCategoryIsUsedException(String message) {
        super(message);
    }
}
