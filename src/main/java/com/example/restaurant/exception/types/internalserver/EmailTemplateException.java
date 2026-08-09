package com.example.restaurant.exception.types.internalserver;

public class EmailTemplateException extends RuntimeException {
    public EmailTemplateException(String message, Exception cause) {
        super(message, cause);
    }
}
