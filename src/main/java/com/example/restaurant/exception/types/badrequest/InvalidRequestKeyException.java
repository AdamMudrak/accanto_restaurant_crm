package com.example.restaurant.exception.types.badrequest;

public class InvalidRequestKeyException extends RuntimeException {
    public InvalidRequestKeyException() {
        super("Idempotency-Key header is required (1-128 letters, digits, _, ., :, -).");
    }
}
