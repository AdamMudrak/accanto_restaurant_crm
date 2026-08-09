package com.example.restaurant.dto.request.management;

import com.example.restaurant.dto.annotations.time.OpenAtBeforeCloseAt;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

@Schema(description = "Payload used to update a restaurant working day.")
@OpenAtBeforeCloseAt
public record UpdateWorkingDayRequest(
        @Schema(description = "Whether this working day is active.", example = "true")
        Boolean isActive,
        @Schema(description = "Opening time for the working day.", example = "09:00:00")
        LocalTime openAt,
        @Schema(description = "Closing time for the working day.", example = "18:00:00")
        LocalTime closeAt
) {}
