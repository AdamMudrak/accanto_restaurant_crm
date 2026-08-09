package com.example.restaurant.dto.request.management;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Request to create an employee shift.")
public record CreateEmployeeShiftRequest(
        @Schema(description = "Employee id assigned to the shift.", example = "2")
        @NotNull
        @Positive
        Long employeeId,
        @Schema(description = "Shift work date.", example = "2026-08-09")
        @NotNull
        LocalDate workDate,
        @Schema(description = "Shift start time.", example = "09:00:00")
        @NotNull
        LocalTime startsAt,
        @Schema(description = "Shift end time.", example = "17:00:00")
        @NotNull
        LocalTime endsAt,
        @Schema(description = "Optional internal shift note.", example = "Morning shift")
        String note
) {}
