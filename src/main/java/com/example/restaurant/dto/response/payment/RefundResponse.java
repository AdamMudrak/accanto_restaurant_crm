package com.example.restaurant.dto.response.payment;

import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.payment.PaymentRefund;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundResponse(Long id, Long paymentId, PaymentRefund.Status status,
                             BigDecimal amount, Restaurant.Currency currency,
                             String providerRefundId, String error, String reason,
                             LocalDateTime createdAt, LocalDateTime completedAt) {}
