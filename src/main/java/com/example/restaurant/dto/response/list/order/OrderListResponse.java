package com.example.restaurant.dto.response.list.order;

import com.example.restaurant.dto.response.order.OrderResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paged order list.")
public record OrderListResponse(
        @Schema(description = "Order items in the current page.")
        List<OrderResponse> orderResponses,
        @Schema(description = "Zero-based page index.", example = "0")
        int pageNumber,
        @Schema(description = "Requested page size.", example = "20")
        int pageSize,
        @Schema(description = "Total number of matching orders.", example = "42")
        long totalElements
) {}
