package com.example.restaurant.exception.types.conflict;

public class NoPendingEmailException extends RuntimeException {
    public NoPendingEmailException(String message) {
        super(message);
    }
}
