package com.example.restaurant.dto.request.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Set;

@Schema(description = "Request to create a cashless payment for selected order items.")
public record PaymentRequest(
        @Schema(description = "Order item ids to include in the payment.", example = "[1, 2]")
        @NotEmpty
        Set<Long> orderItemsIds,
        @Schema(description = "Optional cashless tip amount.", example = "25.00")
        @PositiveOrZero
        BigDecimal tipAmount
) {}
