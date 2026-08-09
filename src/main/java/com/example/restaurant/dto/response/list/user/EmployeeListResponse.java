package com.example.restaurant.dto.response.list.user;

import com.example.restaurant.dto.response.user.EmployeeResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Internal service result for a paginated employee list.")
public record EmployeeListResponse(
        @Schema(description = "Employees on the current page.")
        List<EmployeeResponse> employees,
        @Schema(description = "Zero-based page index.", example = "0")
        int pageNumber,
        @Schema(description = "Requested page size.", example = "20")
        int pageSize,
        @Schema(description = "Total number of matching employees.", example = "42")
        long totalElements
) {}
