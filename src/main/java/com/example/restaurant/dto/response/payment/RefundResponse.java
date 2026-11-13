package com.example.restaurant.dto.response.payment;

import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.payment.PaymentRefund;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Full receipt refund including tips. Timestamps are UTC. "
        + "REQUESTED, PENDING, and UNKNOWN are unresolved; only FAILED may be retried.")
public record RefundResponse(Long id, Long paymentId, PaymentRefund.Status status,
                             BigDecimal amount, Restaurant.Currency currency,
                             String providerRefundId, String error, String reason,
                             LocalDateTime createdAt, LocalDateTime completedAt) {}
