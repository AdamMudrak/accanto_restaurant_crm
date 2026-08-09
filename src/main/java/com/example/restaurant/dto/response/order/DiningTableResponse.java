package com.example.restaurant.dto.response.order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dining table returned by the API.")
public record DiningTableResponse(
        @Schema(description = "Dining table identifier.", example = "1")
        Long id,
        @Schema(
                description = "UUID used to identify the table from guest QR flows.",
                example = "550e8400-e29b-41d4-a716-446655440000")
        String qrUuid,
        @Schema(description = "Whether this dining table is active.", example = "true")
        boolean isActive
) {}
