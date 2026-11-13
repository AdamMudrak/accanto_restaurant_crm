package com.example.restaurant.exception.types.badrequest;

public class MalformedWebhookException extends RuntimeException {
    public MalformedWebhookException() {
        super("Malformed payment webhook payload.");
    }
}
