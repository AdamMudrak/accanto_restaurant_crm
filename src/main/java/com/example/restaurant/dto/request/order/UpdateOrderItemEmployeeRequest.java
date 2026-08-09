package com.example.restaurant.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

@Schema(description = "Employee request to update an order item.")
public record UpdateOrderItemEmployeeRequest(
        @Schema(description = "Updated quantity. Omit to keep unchanged.", example = "3")
        @Positive
        Integer quantity,
        @Schema(description = "Updated item status. Omit to keep unchanged.", example = "CONFIRMED")
        OrderItemStatusDto orderItemStatusDto,
        @Schema(
                description = "Updated optional internal employee comment.",
                example = "Guest confirmed")
        String employeeComment) {

    @Schema(description = "Employee-controlled order item status.")
    public enum OrderItemStatusDto {
        CONFIRMED,
        CANCELED,
        SERVED,
        PAID_CASH
    }
}
