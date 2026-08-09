package com.example.restaurant.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

@Schema(description = "Guest request to update an order item.")
public record UpdateOrderItemRequest(
        @Schema(description = "Updated quantity. Omit to keep unchanged.", example = "3")
        @Positive
        Integer quantity,
        @Schema(description = "Whether the guest wants to cancel the item.", example = "false")
        boolean isCancelled,
        @Schema(description = "Updated optional guest comment.", example = "Sauce on the side")
        String clientComment
) {}
