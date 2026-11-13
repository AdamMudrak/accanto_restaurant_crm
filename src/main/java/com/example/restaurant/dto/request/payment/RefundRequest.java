package com.example.restaurant.dto.request.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Full receipt refund request; includes original tips.")
public record RefundRequest(
        @Schema(description = "Administrator refund reason, up to 1000 characters.",
                example = "Guest requested a full refund.")
        @NotBlank @Size(max = 1000) String reason,

        @Schema(description = "Required true for cash: confirms cash has already been "
                    + "physically returned.",
                example = "true")
        Boolean confirmCashReturned) {}
