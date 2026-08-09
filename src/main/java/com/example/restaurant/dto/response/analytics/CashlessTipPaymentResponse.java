package com.example.restaurant.dto.response.analytics;

import com.example.restaurant.service.payment.PaymentProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Cashless payment tip details for admin analytics.")
public record CashlessTipPaymentResponse(
        @Schema(description = "Payment id.", example = "1")
        Long paymentId,
        @Schema(description = "Order id related to the payment.", example = "1")
        Long orderId,
        @Schema(description = "Employee id assigned to the order.", example = "2")
        Long employeeId,
        @Schema(description = "Employee first name.", example = "employee")
        String employeeFirstName,
        @Schema(description = "Employee last name.", example = "Employee")
        String employeeLastName,
        @Schema(description = "Guest id that initiated the payment.", example = "1")
        Long payerGuestId,
        @Schema(description = "Cashless payment provider.", example = "STRIPE")
        PaymentProviderType providerType,
        @Schema(description = "Tip amount included in the cashless payment.", example = "45.00")
        BigDecimal tipAmount,
        @Schema(description = "Whether the tip was paid out to the employee.", example = "false")
        boolean tipPaid,
        @Schema(description = "Date and time when the tip was marked as paid.")
        LocalDateTime tipPaidAt,
        @Schema(description = "Date and time when the payment was completed.")
        LocalDateTime completedAt
) {}
