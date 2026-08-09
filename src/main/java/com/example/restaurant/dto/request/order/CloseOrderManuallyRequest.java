package com.example.restaurant.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Employee request to manually close an order.")
public record CloseOrderManuallyRequest(
        @Schema(
                description = "Status to apply to unpaid order items before closing the order.",
                example = "PAID_CASH")
        @NotNull
        ManualCloseStatus markUnpaidAs,
        @Schema(description = "Optional internal close comment.", example = "Paid at the bar")
        String employeeComment
) {
    @Schema(description = "Manual close status for unpaid order items.")
    public enum ManualCloseStatus {
        PAID_CASH,
        CANCELED
    }
}
