package com.example.restaurant.service.order.order;

import com.example.restaurant.dto.request.order.AddOrderItemRequest;
import com.example.restaurant.dto.request.order.UpdateOrderItemRequest;
import com.example.restaurant.dto.response.order.CurrentGuestOrderResponse;
import com.example.restaurant.dto.response.order.OrderResponse;
import com.example.restaurant.entity.menu.MenuItem;
import com.example.restaurant.entity.order.Guest;
import com.example.restaurant.entity.order.Order;
import com.example.restaurant.entity.order.OrderItem;
import com.example.restaurant.exception.types.conflict.GuestIllegalStateException;
import com.example.restaurant.exception.types.conflict.ItemIllegalStateException;
import com.example.restaurant.exception.types.notfound.GuestNotFoundException;
import com.example.restaurant.exception.types.notfound.MenuItemNotFoundException;
import com.example.restaurant.exception.types.notfound.OrderItemNotFoundException;
import com.example.restaurant.exception.types.notfound.OrderNotFoundException;
import com.example.restaurant.mapper.order.OrderItemMapper;
import com.example.restaurant.mapper.order.OrderMapper;
import com.example.restaurant.repository.menu.MenuItemRepository;
import com.example.restaurant.repository.order.GuestRepository;
import com.example.restaurant.repository.order.OrderItemRepository;
import com.example.restaurant.repository.order.OrderRepository;
import com.example.restaurant.service.utils.CookieExtractor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderGuestService {
    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;
    private final OrderItemRepository orderItemRepository;
    private final GuestRepository guestRepository;

    @Transactional(readOnly = true)
    public CurrentGuestOrderResponse getCurrentOrder(HttpServletRequest request) {
        Order order = findOrderByUuid(request);
        Guest guest = findGuest(request);
        validateGuestBelongsToOrder(guest, order);

        return getCurrentGuestOrderResponse(order, guest);
    }

    public CurrentGuestOrderResponse addOrderItem(
            HttpServletRequest request, AddOrderItemRequest addOrderItemRequest) {
        Order order = findOrderByUuid(request);
        Guest guest = findGuest(request);
        validateGuestBelongsToOrder(guest, order);
        MenuItem menuItem = findMenuItem(addOrderItemRequest.menuItemId());

        int currentQuantity =
                getCurrentMenuItemQuantityPerGuest(order.getId(), guest.getId(), menuItem.getId());

        if (menuItem.getMaxQuantityPerPerson() != null
                && menuItem.getMaxQuantityPerPerson()
                    < (addOrderItemRequest.quantity() + currentQuantity)) {
            throw new ItemIllegalStateException("Quantity for item "
                    + menuItem.getNameDefaultLanguage() + " exceeds maximum quantity per person.");
        }

        OrderItem orderItem = orderItemMapper.toCreateOrderItem(
                guest, order, menuItem, addOrderItemRequest);

        orderItemRepository.save(orderItem);
        orderItemRepository.flush();

        return getCurrentGuestOrderResponse(order, guest);
    }

    public CurrentGuestOrderResponse updateOrderItem(Long id,
            HttpServletRequest request, UpdateOrderItemRequest updateOrderItemRequest) {
        Order order = findOrderByUuid(request);
        Guest guest = findGuest(request);
        validateGuestBelongsToOrder(guest, order);

        OrderItem orderItem = findOrderItem(id, guest.getId(), order.getId());
        validateGuestCanUpdateOrderItem(orderItem);

        MenuItem menuItem = orderItem.getMenuItem();

        int currentQuantity =
                getCurrentMenuItemQuantityPerGuest(order.getId(), guest.getId(), menuItem.getId())
                        - orderItem.getQuantity();

        if (menuItem.getMaxQuantityPerPerson() != null && updateOrderItemRequest.quantity() != null
                && menuItem.getMaxQuantityPerPerson()
                < (currentQuantity + updateOrderItemRequest.quantity())) {
            throw new ItemIllegalStateException(
                    "Quantity for item " + menuItem.getNameDefaultLanguage()
                    + " exceeds maximum quantity per person.");
        }
        updateOrderItemPresentFields(orderItem, updateOrderItemRequest);

        orderItemRepository.save(orderItem);
        orderItemRepository.flush();

        return getCurrentGuestOrderResponse(order, guest);
    }

    private Order findOrderByUuid(HttpServletRequest request) {
        String orderUuid = CookieExtractor.extractCookieValueByName(request, "orderUuid");
        return orderRepository
                .findByUuidAndStatus(
                        orderUuid, Order.OrderStatus.OPENED)
                .orElseThrow(() -> new OrderNotFoundException(
                        "No opened with uuid: " + orderUuid + " found."));
    }

    private MenuItem findMenuItem(Long menuItemId) {
        return menuItemRepository.findByIdAndIsAvailableTrue(menuItemId).orElseThrow(() ->
                new MenuItemNotFoundException("MenuItem with id: " + menuItemId + " not found.")
        );
    }

    private OrderItem findOrderItem(Long itemId, Long guestId, Long orderId) {
        return orderItemRepository.findByIdAndGuestIdAndOrderId(itemId, guestId, orderId)
                .orElseThrow(() -> new OrderItemNotFoundException(
                        "Order item with id: " + itemId
                                + " not found for guest with id: " + guestId
                                + " and order with id: " + orderId + "."));
    }

    private void updateOrderItemPresentFields(OrderItem orderItem,
                                              UpdateOrderItemRequest updateOrderItemRequest) {
        if (updateOrderItemRequest.clientComment() != null
                && !updateOrderItemRequest.clientComment().isBlank()) {
            orderItem.setClientNote(updateOrderItemRequest.clientComment());
        }

        if (updateOrderItemRequest.isCancelled()) {
            orderItem.setStatus(OrderItem.OrderItemStatus.CANCELED);
        }

        if (updateOrderItemRequest.quantity() != null
                && updateOrderItemRequest.quantity() > 0) {
            orderItem.setQuantity(updateOrderItemRequest.quantity());
        }
    }

    private Guest findGuest(HttpServletRequest request) {
        String guestUuid = CookieExtractor.extractCookieValueByName(request, "guestUuid");
        return guestRepository.findByUuid(guestUuid).orElseThrow(
                () -> new GuestNotFoundException("Guest with uuid: " + guestUuid + " not found."));
    }

    private void validateGuestCanUpdateOrderItem(OrderItem orderItem) {
        if (!orderItem.getStatus().equals(OrderItem.OrderItemStatus.ORDERED)) {
            throw new ItemIllegalStateException(
                    "OrderItem is already confirmed and cannot be updated by customer");
        }
    }

    private int getCurrentMenuItemQuantityPerGuest(Long orderId, Long guestId, Long menuItemId) {
        return orderItemRepository.findAllByOrderIdAndGuestIdAndMenuItemIdAndStatusNot(
                        orderId, guestId, menuItemId, OrderItem.OrderItemStatus.CANCELED)
                .stream()
                .map(OrderItem::getQuantity)
                .reduce(0, Integer::sum);
    }

    private void validateGuestBelongsToOrder(Guest guest, Order order) {
        if (!guest.getOrder().getId().equals(order.getId())) {
            throw new GuestIllegalStateException("Guest " + guest.getId()
                    + " is not associated with order id " + order.getId());
        }
    }

    private CurrentGuestOrderResponse getCurrentGuestOrderResponse(Order order, Guest guest) {
        OrderResponse orderResponse = orderMapper.toOrderResponse(order);
        orderResponse.setOrderItemsByGuests(orderItemMapper.toResponseOrderItemsByGuests(
                orderItemRepository.findAllByOrderId(order.getId()), guest.getId()));

        return new CurrentGuestOrderResponse(guest.getId(), orderResponse);
    }
}
