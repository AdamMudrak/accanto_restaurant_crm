package com.example.restaurant.service.payment;

import com.example.restaurant.entity.payment.PaymentRefund;

public record ProviderRefund(String id, PaymentRefund.Status status, long amount,
        String currency) {}
