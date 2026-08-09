package com.example.restaurant.dto.response.list.management;

import com.example.restaurant.dto.response.management.WorkingDayResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Internal service result for the restaurant working day list.")
public record WorkingDayListResponse(
        @Schema(description = "Working days returned by the service.")
        List<WorkingDayResponse> workingDays,
        @Schema(description = "Zero-based page index.", example = "0")
        int pageNumber,
        @Schema(description = "Page size used for the response.", example = "7")
        int pageSize,
        @Schema(description = "Total number of working days.", example = "7")
        long totalElements
) {}
