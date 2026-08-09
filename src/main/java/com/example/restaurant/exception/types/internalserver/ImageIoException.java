package com.example.restaurant.exception.types.internalserver;

public class ImageIoException extends RuntimeException {
    public ImageIoException(String message, Exception cause) {
        super(message, cause);
    }

}
