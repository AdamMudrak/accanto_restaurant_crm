package com.example.restaurant.service.payment.stripe.dto.request;

public record StripeCheckoutSessionRequest(
        Long paymentId,
        Long amount,
        String currency,
        String productName,
        String successUrl,
        String cancelUrl
) {}
