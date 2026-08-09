package com.example.restaurant.dto.response.list.analytics;

import com.example.restaurant.dto.response.analytics.CashlessTipPaymentResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paged cashless tip payment list.")
public record CashlessTipPaymentListResponse(
        @Schema(description = "Cashless tip payment items.")
        List<CashlessTipPaymentResponse> items,
        @Schema(description = "Zero-based page index.", example = "0")
        int pageNumber,
        @Schema(description = "Requested page size.", example = "20")
        int pageSize,
        @Schema(description = "Total number of matching cashless tip payments.", example = "42")
        long totalElements
) {}
