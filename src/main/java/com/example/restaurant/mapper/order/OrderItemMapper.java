package com.example.restaurant.mapper.order;

import com.example.restaurant.dto.request.order.AddOrderItemEmployeeRequest;
import com.example.restaurant.dto.request.order.AddOrderItemRequest;
import com.example.restaurant.dto.response.order.OrderItemResponse;
import com.example.restaurant.dto.response.order.OrderItemsByGuestResponse;
import com.example.restaurant.entity.menu.MenuItem;
import com.example.restaurant.entity.order.Guest;
import com.example.restaurant.entity.order.Order;
import com.example.restaurant.entity.order.OrderItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper {
    public OrderItem toCreateOrderItem(Guest guest, Order order,
                                       MenuItem menuItem, AddOrderItemRequest addOrderItemRequest) {
        OrderItem orderItem = new OrderItem();
        orderItem.setGuest(guest);
        orderItem.setOrder(order);
        orderItem.setMenuItem(menuItem);
        orderItem.setItemNameSnapshot(menuItem.getNameDefaultLanguage());
        orderItem.setItemPriceSnapshot(menuItem.getPrice());
        orderItem.setQuantity(addOrderItemRequest.quantity());
        orderItem.setClientNote(addOrderItemRequest.clientComment());

        return orderItem;
    }

    public OrderItem toCreateOrderItemEmployee(
            Guest guest, Order order, MenuItem menuItem,
            AddOrderItemEmployeeRequest addOrderItemEmployeeRequest) {
        OrderItem orderItem = new OrderItem();
        orderItem.setGuest(guest);
        orderItem.setOrder(order);
        orderItem.setMenuItem(menuItem);
        orderItem.setItemNameSnapshot(menuItem.getNameDefaultLanguage());
        orderItem.setItemPriceSnapshot(menuItem.getPrice());
        orderItem.setQuantity(addOrderItemEmployeeRequest.quantity());
        orderItem.setEmployeeNote(addOrderItemEmployeeRequest.employeeComment());

        return orderItem;
    }

    public OrderItemResponse toResponseOrderItem(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getGuest().getId(),
                orderItem.getOrder().getId(),
                orderItem.getMenuItem().getId(),
                orderItem.getItemNameSnapshot(),
                orderItem.getItemPriceSnapshot(),
                orderItem.getQuantity(),
                orderItem.getStatus(),
                orderItem.getClientNote(),
                orderItem.getEmployeeNote()
        );
    }

    public List<OrderItemsByGuestResponse> toResponseOrderItemsByGuests(
            List<OrderItem> orderItems) {
        return toResponseOrderItemsByGuests(orderItems, null);
    }

    public List<OrderItemsByGuestResponse> toResponseOrderItemsByGuests(
            List<OrderItem> orderItems, Long firstGuestId) {
        Map<Long, List<OrderItemResponse>> orderItemsByGuestId = new LinkedHashMap<>();
        for (OrderItem orderItem : orderItems) {
            Long guestId = orderItem.getGuest().getId();
            orderItemsByGuestId.computeIfAbsent(guestId, id -> new ArrayList<>())
                    .add(toResponseOrderItem(orderItem));
        }

        return orderItemsByGuestId.entrySet().stream()
                .sorted((firstEntry, secondEntry) -> compareGuestIds(
                        firstEntry.getKey(), secondEntry.getKey(), firstGuestId))
                .map(entry -> new OrderItemsByGuestResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private int compareGuestIds(Long firstGuestId, Long secondGuestId, Long preferredGuestId) {
        if (preferredGuestId == null) {
            return 0;
        }
        if (firstGuestId.equals(preferredGuestId)) {
            return -1;
        }
        if (secondGuestId.equals(preferredGuestId)) {
            return 1;
        }

        return 0;
    }
}
