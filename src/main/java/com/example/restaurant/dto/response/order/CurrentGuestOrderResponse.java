package com.example.restaurant.dto.response.order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current guest view of the full opened order.")
public record CurrentGuestOrderResponse(
        @Schema(description = "Guest id from current guest cookies.", example = "7")
        Long thisGuestId,
        @Schema(description = "Full opened order. Current guest items are listed first.")
        OrderResponse order
) {}
