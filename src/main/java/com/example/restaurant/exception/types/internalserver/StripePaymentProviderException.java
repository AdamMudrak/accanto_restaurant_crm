package com.example.restaurant.exception.types.internalserver;

public class StripePaymentProviderException extends RuntimeException {
    public StripePaymentProviderException(String message) {
        super(message);
    }

    public StripePaymentProviderException(int code, String body) {
        super("Stripe API error. Status code: " + code + ", body: " + body);
    }
}
