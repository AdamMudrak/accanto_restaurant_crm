package com.example.restaurant.dto.request.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotNull @Positive Long paymentId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 2000) String comment,
        @Schema(description = "One-use cash receipt token; omit for online payer cookies.")
        @Size(max = 64) String token
) {}
