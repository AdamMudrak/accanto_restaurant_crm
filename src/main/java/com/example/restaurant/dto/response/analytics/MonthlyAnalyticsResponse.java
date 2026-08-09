package com.example.restaurant.dto.response.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Monthly cashless analytics summary.")
public record MonthlyAnalyticsResponse(
        @Schema(description = "Analytics year.", example = "2026")
        int year,
        @Schema(description = "Analytics month number from 1 to 12.", example = "8")
        int month,
        @Schema(description = "Total successful cashless payment revenue.", example = "4250.00")
        BigDecimal revenueAmount,
        @Schema(description = "Total successful cashless tips.", example = "540.00")
        BigDecimal tipAmount,
        @Schema(description = "Number of successful cashless payments.", example = "17")
        long succeededPaymentsCount
) {}
