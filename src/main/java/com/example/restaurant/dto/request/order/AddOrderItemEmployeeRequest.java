package com.example.restaurant.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Employee request to add a menu item to an order.")
public record AddOrderItemEmployeeRequest(
        @Schema(
                description = "Guest id for the item. Omit when item is not assigned.",
                example = "1")
        Long guestId,
        @Schema(description = "Menu item id to add.", example = "1")
        @NotNull
        @Positive
        Long menuItemId,
        @Schema(description = "Quantity of the menu item.", example = "2")
        @NotNull
        @Positive
        Integer quantity,
        @Schema(description = "Optional internal employee comment.", example = "Fire with mains")
        String employeeComment
) {}
