package com.example.restaurant.dto.response.list.order;

import com.example.restaurant.dto.response.order.DiningTableResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Internal service result for a paginated dining table list.")
public record DiningTableListResponse(
        @Schema(description = "Dining tables on the current page.")
        List<DiningTableResponse> items,
        @Schema(description = "Zero-based page index.", example = "0")
        int pageNumber,
        @Schema(description = "Requested page size.", example = "20")
        int pageSize,
        @Schema(description = "Total number of matching dining tables.", example = "42")
        long totalElements
) {}
