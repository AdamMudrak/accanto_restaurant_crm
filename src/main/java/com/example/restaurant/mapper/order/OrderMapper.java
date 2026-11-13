package com.example.restaurant.mapper.order;

import com.example.restaurant.dto.response.order.OrderResponse;
import com.example.restaurant.entity.order.DiningTable;
import com.example.restaurant.entity.order.Order;
import com.example.restaurant.repository.management.RestaurantRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderMapper {
    private final RestaurantRepository restaurantRepository;

    public Order toCreateOrder(Long diningTableId) {
        Order order = new Order();
        order.setCurrency(restaurantRepository.findSingleton().orElseThrow().getCurrency());
        order.setDiningTable(createDiningTable(diningTableId));
        order.setUuid(UUID.randomUUID().toString());
        order.setCreatedAt(LocalDateTime.now());

        return order;
    }

    public OrderResponse toOrderResponse(Order order) {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setId(order.getId());
        orderResponse.setCurrency(order.getCurrency());
        orderResponse.setUuid(order.getUuid());
        orderResponse.setOrderStatus(order.getStatus());
        orderResponse.setDiningTableId(order.getDiningTable().getId());
        orderResponse.setEmployeeId(getEmployeeId(order));
        orderResponse.setCreatedAt(order.getCreatedAt());
        orderResponse.setClosedAt(order.getClosedAt());

        return orderResponse;
    }

    public List<OrderResponse> toOrderResponseList(List<Order> orders) {
        List<OrderResponse> orderResponseList = new ArrayList<>();
        for (Order order : orders) {
            orderResponseList.add(toOrderResponse(order));
        }
        return orderResponseList;
    }

    private DiningTable createDiningTable(Long diningTableId) {
        return new DiningTable(diningTableId);
    }

    private Long getEmployeeId(Order order) {
        if (order.getEmployee() != null) {
            return order.getEmployee().getId();
        }
        return null;
    }
}
