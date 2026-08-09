package com.example.restaurant.dto.response.order;

import com.example.restaurant.entity.order.Order;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Order response with order items grouped by guest id.")
@Getter
@Setter
public class OrderResponse {
    @Schema(description = "Order id.", example = "1")
    private Long id;
    @Schema(description = "Public order UUID.", example = "00000000-0000-0000-0000-000000000101")
    private String uuid;
    @Schema(description = "Order status.", example = "CLOSED")
    private Order.OrderStatus orderStatus;
    @Schema(description = "Dining table id assigned to the order.", example = "1")
    private Long diningTableId;
    @Schema(description = "Employee id assigned to the order.", example = "2")
    private Long employeeId;
    @Schema(description = "Date and time when the order was created.")
    private LocalDateTime createdAt;
    @Schema(description = "Date and time when the order was closed.")
    private LocalDateTime closedAt;
    @Schema(description = "Order items grouped by guest.")
    private List<OrderItemsByGuestResponse> orderItemsByGuests = null;
}
