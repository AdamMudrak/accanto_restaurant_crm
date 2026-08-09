package com.example.restaurant.dto.response.order;

import com.example.restaurant.entity.order.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Order item response.")
public record OrderItemResponse(
        @Schema(description = "Order item id.", example = "1")
        Long id,
        @Schema(description = "Guest id assigned to the item.", example = "1")
        Long guestId,
        @Schema(description = "Order id.", example = "1")
        Long orderId,
        @Schema(description = "Menu item id.", example = "1")
        Long menuItemId,
        @Schema(description = "Menu item name snapshot at ordering time.", example = "Caesar Salad")
        String itemNameSnapshot,
        @Schema(description = "Menu item price snapshot at ordering time.", example = "180.00")
        BigDecimal itemPriceSnapshot,
        @Schema(description = "Ordered item quantity.", example = "2")
        int quantity,
        @Schema(description = "Order item status.", example = "CONFIRMED")
        OrderItem.OrderItemStatus status,
        @Schema(description = "Optional guest comment.", example = "No onions please")
        String clientComment,
        @Schema(description = "Optional internal employee comment.", example = "Fire with mains")
        String employeeComment
) {}
