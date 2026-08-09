package com.example.restaurant.dto.response.management;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "Employee shift schedule response.")
public record EmployeeShiftResponse(
        @Schema(description = "Employee shift id.", example = "1")
        Long id,
        @Schema(description = "Employee id assigned to the shift.", example = "2")
        Long employeeId,
        @Schema(description = "Employee first name.", example = "employee")
        String employeeFirstName,
        @Schema(description = "Employee last name.", example = "Employee")
        String employeeLastName,
        @Schema(description = "Shift work date.", example = "2026-08-09")
        LocalDate workDate,
        @Schema(description = "Shift start time.", example = "09:00:00")
        LocalTime startsAt,
        @Schema(description = "Shift end time.", example = "17:00:00")
        LocalTime endsAt,
        @Schema(description = "Optional internal shift note.", example = "Morning shift")
        String note,
        @Schema(description = "Date and time when the shift was created.")
        LocalDateTime createdAt,
        @Schema(description = "Date and time when the shift was last updated.")
        LocalDateTime updatedAt
) {}
