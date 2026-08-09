package com.example.restaurant.dto.response.management;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

@Schema(description = "Restaurant working day returned by the API.")
public record WorkingDayResponse(
        @Schema(description = "Working day identifier.", example = "1")
        Long id,
        @Schema(description = "Day of week.", example = "MONDAY")
        String day,
        @Schema(description = "Whether this working day is active.", example = "true")
        boolean isActive,
        @Schema(description = "Opening time.", example = "09:00:00")
        LocalTime openAt,
        @Schema(description = "Closing time.", example = "18:00:00")
        LocalTime closeAt
) {}
