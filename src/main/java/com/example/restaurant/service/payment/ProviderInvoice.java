package com.example.restaurant.service.payment;

import com.example.restaurant.entity.payment.Payment;
import java.time.OffsetDateTime;

public record ProviderInvoice(String id, String reference, long amount, String currency,
                              Payment.PaymentStatus status, OffsetDateTime modifiedAt) {}
