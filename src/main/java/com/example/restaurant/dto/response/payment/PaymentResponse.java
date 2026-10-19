package com.example.restaurant.dto.response.payment;

import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.entity.payment.PaymentRefund;
import com.example.restaurant.service.payment.PaymentProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Online or cash payment response.")
public record PaymentResponse(
        @Schema(description = "Payment id.", example = "1")
        Long paymentId,
        @Schema(description = "Order id related to the payment.", example = "1")
        Long orderId,
        @Schema(description = "Guest id that initiated the payment.", example = "1")
        Long payerGuestId,
        @Schema(description = "Online payment provider; null for cash.", example = "STRIPE")
        PaymentProviderType providerType,
        @Schema(description = "Provider invoice id.", example = "inv_123")
        String invoiceId,
        @Schema(description = "Provider checkout page URL.")
        String pageUrl,
        @Schema(description = "Payment status.", example = "SUCCEEDED")
        Payment.PaymentStatus status,
        @Schema(description = "Order amount without tips.", example = "250.00")
        BigDecimal orderAmount,
        @Schema(description = "Cashless tip amount.", example = "25.00")
        BigDecimal tipAmount,
        @Schema(description = "Date and time when the payment was created.")
        LocalDateTime createdAt,
        @Schema(description = "Date and time when the payment was completed.")
        LocalDateTime completedAt,
        @Schema(description = "Currency. Null for unresolved historical records.")
        Restaurant.Currency currency,
        Payment.PaymentMethod method,
        boolean reconstructed,
        String reconciliationIssue,
        String lastSyncError,
        String resolutionNote,
        PaymentRefund.Status refundStatus
) {}
