package com.example.restaurant.dto.response.list.management;

import com.example.restaurant.dto.response.management.EmployeeShiftResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paged employee shift list.")
public record EmployeeShiftListResponse(
        @Schema(description = "Employee shift items.")
        List<EmployeeShiftResponse> items,
        @Schema(description = "Zero-based page index.", example = "0")
        int pageNumber,
        @Schema(description = "Requested page size.", example = "20")
        int pageSize,
        @Schema(description = "Total number of matching employee shifts.", example = "42")
        long totalElements
) {}
