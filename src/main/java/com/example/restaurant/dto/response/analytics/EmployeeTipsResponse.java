package com.example.restaurant.dto.response.analytics;

import com.example.restaurant.entity.management.Restaurant;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Cashless tip totals grouped by employee.")
public record EmployeeTipsResponse(
        @Schema(description = "Employee id.", example = "2")
        Long employeeId,
        @Schema(description = "Employee first name.", example = "employee")
        String firstName,
        @Schema(description = "Employee last name.", example = "Employee")
        String lastName,
        @Schema(description = "Total cashless tips assigned to the employee.", example = "180.00")
        BigDecimal totalTipAmount,
        @Schema(description = "Cashless tips already marked as paid.", example = "120.00")
        BigDecimal paidTipAmount,
        @Schema(description = "Cashless tips not yet marked as paid.", example = "60.00")
        BigDecimal unpaidTipAmount,
        @Schema(description = "Number of cashless payments with tips.", example = "4")
        long paymentCount,
        Restaurant.Currency currency
) {}
