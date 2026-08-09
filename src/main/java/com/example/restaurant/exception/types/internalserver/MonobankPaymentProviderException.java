package com.example.restaurant.exception.types.internalserver;

public class MonobankPaymentProviderException extends RuntimeException {
    public MonobankPaymentProviderException(String message) {
        super(message);
    }

    public MonobankPaymentProviderException(int code, String body) {
        super("code: " + code + ", body: " + body);
    }
}
