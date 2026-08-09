package com.example.restaurant.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Guest request to add a menu item to the current order.")
public record AddOrderItemRequest(
        @Schema(description = "Menu item id to add.", example = "1")
        @NotNull
        @Positive
        Long menuItemId,
        @Schema(description = "Quantity of the menu item.", example = "2")
        @NotNull
        @Positive
        Integer quantity,
        @Schema(description = "Optional comment from the guest.", example = "No onions please")
        String clientComment
) {}
