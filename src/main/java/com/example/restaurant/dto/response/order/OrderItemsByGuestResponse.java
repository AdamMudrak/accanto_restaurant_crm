package com.example.restaurant.dto.response.order;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Order items grouped under an explicit guest id.")
public record OrderItemsByGuestResponse(
        @Schema(description = "Guest id assigned to these order items.", example = "7")
        Long guestId,
        @Schema(description = "Order items assigned to the guest.")
        List<OrderItemResponse> orderItems
) {}
