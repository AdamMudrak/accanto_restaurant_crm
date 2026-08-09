package com.example.restaurant.dto.response.list.payment;

import com.example.restaurant.dto.response.payment.PaymentResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paged cashless payment list.")
public record PaymentListResponse(
        @Schema(description = "Cashless payment items.")
        List<PaymentResponse> items,
        @Schema(description = "Zero-based page index.", example = "0")
        int pageNumber,
        @Schema(description = "Requested page size.", example = "20")
        int pageSize,
        @Schema(description = "Total number of matching payments.", example = "42")
        long totalElements
) {}
