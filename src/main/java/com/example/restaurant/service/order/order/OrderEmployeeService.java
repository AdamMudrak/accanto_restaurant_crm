package com.example.restaurant.service.order.order;

import com.example.restaurant.dto.request.order.AddOrderItemEmployeeRequest;
import com.example.restaurant.dto.request.order.CloseOrderManuallyRequest;
import com.example.restaurant.dto.request.order.UpdateOrderItemEmployeeRequest;
import com.example.restaurant.dto.response.list.order.OrderListResponse;
import com.example.restaurant.dto.response.order.OrderItemsByGuestResponse;
import com.example.restaurant.dto.response.order.OrderResponse;
import com.example.restaurant.entity.menu.MenuItem;
import com.example.restaurant.entity.order.Guest;
import com.example.restaurant.entity.order.Order;
import com.example.restaurant.entity.order.OrderItem;
import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.exception.types.conflict.GuestIllegalStateException;
import com.example.restaurant.exception.types.conflict.ItemIllegalStateException;
import com.example.restaurant.exception.types.conflict.OrderIllegalStateException;
import com.example.restaurant.exception.types.notfound.GuestNotFoundException;
import com.example.restaurant.exception.types.notfound.MenuItemNotFoundException;
import com.example.restaurant.exception.types.notfound.OrderItemNotFoundException;
import com.example.restaurant.exception.types.notfound.OrderNotFoundException;
import com.example.restaurant.mapper.order.GuestMapper;
import com.example.restaurant.mapper.order.OrderItemMapper;
import com.example.restaurant.mapper.order.OrderMapper;
import com.example.restaurant.repository.menu.MenuItemRepository;
import com.example.restaurant.repository.order.GuestRepository;
import com.example.restaurant.repository.order.OrderItemRepository;
import com.example.restaurant.repository.order.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderEmployeeService {
    private static final Set<OrderItem.OrderItemStatus> ALLOWED_STATUSES_TO_CLOSE_ORDER =
            Set.of(OrderItem.OrderItemStatus.PAID_CASH,
                   OrderItem.OrderItemStatus.PAID_CARD,
                   OrderItem.OrderItemStatus.CANCELED);
    private static final Set<OrderItem.OrderItemStatus> FINAL_ORDER_ITEM_STATUSES =
            Set.of(OrderItem.OrderItemStatus.PAID_CASH,
                   OrderItem.OrderItemStatus.PAID_CARD,
                   OrderItem.OrderItemStatus.CANCELED);
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final GuestMapper guestMapper;
    private final GuestRepository guestRepository;
    private final MenuItemRepository menuItemRepository;

    public OrderListResponse getAllOpenOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAllByStatus(Order.OrderStatus.OPENED, pageable);

        List<OrderResponse> orderResponses = orderMapper.toOrderResponseList(orders.getContent());
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        long totalElements = orders.getTotalElements();

        return new OrderListResponse(orderResponses, pageNumber, pageSize, totalElements);
    }

    @Transactional(readOnly = true)
    public OrderListResponse getOrderHistory(
            Order.OrderStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {
        Page<Order> orders = orderRepository.findHistory(status, from, to, pageable);

        List<OrderResponse> orderResponses = orderMapper.toOrderResponseList(orders.getContent());
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        long totalElements = orders.getTotalElements();

        return new OrderListResponse(orderResponses, pageNumber, pageSize, totalElements);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = findOrderById(orderId);

        List<OrderItemsByGuestResponse> orderItemResponsesByGuest =
                orderItemMapper.toResponseOrderItemsByGuests(
                        orderItemRepository.findAllByOrderId(order.getId()));

        OrderResponse orderResponse = orderMapper.toOrderResponse(order);
        orderResponse.setOrderItemsByGuests(orderItemResponsesByGuest);

        return orderResponse;
    }

    public List<OrderItemsByGuestResponse> addOrderItem(Long orderId,
                                       AddOrderItemEmployeeRequest addOrderItemEmployeeRequest) {
        Order order = findOrderById(orderId);
        Guest guest;

        if (addOrderItemEmployeeRequest.guestId() == null) {
            guest = guestMapper.toCreateGuest(order);
            guestRepository.save(guest);
            guestRepository.flush();
        } else {
            guest = findGuestById(addOrderItemEmployeeRequest.guestId());
            if (!guest.getOrder().getId().equals(order.getId())) {
                throw new GuestIllegalStateException("Guest with id " + guest.getId()
                        + " doesn't belong to order" + order.getId());
            }
        }
        MenuItem menuItem = findMenuItemById(addOrderItemEmployeeRequest.menuItemId());

        OrderItem orderItem = orderItemMapper.toCreateOrderItemEmployee(
                guest, order, menuItem, addOrderItemEmployeeRequest);

        orderItemRepository.save(orderItem);
        orderItemRepository.flush();

        return orderItemMapper.toResponseOrderItemsByGuests(
                orderItemRepository.findAllByOrderId(order.getId()));
    }

    public List<OrderItemsByGuestResponse> updateOrderItem(Long orderItemId,
            UpdateOrderItemEmployeeRequest updateOrderItemEmployeeRequest,
            Employee employee) {
        OrderItem orderItem = findOrderItemById(orderItemId);

        if (updateOrderItemEmployeeRequest.quantity() != null) {
            if (!orderItem.getStatus().equals(OrderItem.OrderItemStatus.ORDERED)) {
                throw new ItemIllegalStateException(
                        "Cannot update order item quantity because of its status");
            }

            orderItem.setQuantity(updateOrderItemEmployeeRequest.quantity());
        }

        if (updateOrderItemEmployeeRequest.employeeComment() != null
                && !updateOrderItemEmployeeRequest.employeeComment().isBlank()) {
            orderItem.setEmployeeNote(updateOrderItemEmployeeRequest.employeeComment());
        }

        if (updateOrderItemEmployeeRequest.orderItemStatusDto() != null) {
            updateOrderItemStatus(
                    orderItem, updateOrderItemEmployeeRequest.orderItemStatusDto(), employee);
        }

        orderItemRepository.save(orderItem);
        orderItemRepository.flush();

        return orderItemMapper.toResponseOrderItemsByGuests(
                orderItemRepository.findAllByOrderId(orderItem.getOrder().getId()));
    }

    public OrderResponse closeOrder(Long id) {
        Order order = findOrderById(id);
        allowOnlyCloseOrder(order);

        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    public OrderResponse closeOrderManually(Long id, CloseOrderManuallyRequest request) {
        Order order = findOrderById(id);
        OrderItem.OrderItemStatus statusToApply =
                OrderItem.OrderItemStatus.valueOf(request.markUnpaidAs().name());

        orderItemRepository.findAllByOrderId(order.getId())
                .forEach(orderItem -> markUnpaidOrderItem(orderItem, statusToApply, request));
        markOrderClosed(order);

        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    private Order findOrderById(Long orderId) {
        return orderRepository
                .findByIdAndStatus(
                        orderId, Order.OrderStatus.OPENED)
                .orElseThrow(() -> new OrderNotFoundException(
                        "No opened order with id" + orderId));
    }

    private Guest findGuestById(Long guestId) {
        return guestRepository.findById(guestId).orElseThrow(
                () -> new GuestNotFoundException(
                        "Can't find guest with id: " + guestId));
    }

    private MenuItem findMenuItemById(Long menuItemId) {
        return menuItemRepository.findByIdAndIsAvailableTrue(menuItemId).orElseThrow(
                () -> new MenuItemNotFoundException(
                        "Can't find menu item with id: " + menuItemId));
    }

    private OrderItem findOrderItemById(Long orderItemId) {
        return orderItemRepository.findByIdAndOrder_Status(orderItemId, Order.OrderStatus.OPENED)
                .orElseThrow(() -> new OrderItemNotFoundException(
                        "Can't find order item with id: " + orderItemId));
    }

    private void updateOrderItemStatus(
            OrderItem orderItem,
            UpdateOrderItemEmployeeRequest.OrderItemStatusDto orderItemStatusDto,
            Employee employee) {
        OrderItem.OrderItemStatus newStatus = OrderItem.OrderItemStatus.valueOf(
                orderItemStatusDto.name());
        validateOrderItemStatusTransition(orderItem.getStatus(), newStatus);
        orderItem.setStatus(newStatus);
        assignEmployeeOnConfirmation(orderItem, newStatus, employee);
    }

    private void validateOrderItemStatusTransition(OrderItem.OrderItemStatus currentStatus,
                                                   OrderItem.OrderItemStatus newStatus) {
        if (currentStatus.equals(newStatus)) {
            return;
        }

        if (FINAL_ORDER_ITEM_STATUSES.contains(currentStatus)) {
            throw new ItemIllegalStateException(
                    "Cannot change final order item status " + currentStatus);
        }

        boolean isAllowed = switch (currentStatus) {
            case ORDERED -> Set.of(
                    OrderItem.OrderItemStatus.CONFIRMED,
                    OrderItem.OrderItemStatus.CANCELED,
                    OrderItem.OrderItemStatus.PAID_CASH,
                    OrderItem.OrderItemStatus.PAID_CARD
            ).contains(newStatus);
            case CONFIRMED -> Set.of(
                    OrderItem.OrderItemStatus.SERVED,
                    OrderItem.OrderItemStatus.CANCELED,
                    OrderItem.OrderItemStatus.PAID_CASH,
                    OrderItem.OrderItemStatus.PAID_CARD
            ).contains(newStatus);
            case SERVED -> Set.of(
                    OrderItem.OrderItemStatus.PAID_CASH,
                    OrderItem.OrderItemStatus.PAID_CARD
            ).contains(newStatus);
            case CANCELED, PAID_CASH, PAID_CARD -> false;
        };

        if (!isAllowed) {
            throw new ItemIllegalStateException(
                    "Cannot change order item status from "
                            + currentStatus + " to " + newStatus);
        }
    }

    private void allowOnlyCloseOrder(Order order) {
        orderItemRepository.findAllByOrderId(order.getId())
                .forEach(orderItem -> {
                    if (!ALLOWED_STATUSES_TO_CLOSE_ORDER.contains(orderItem.getStatus())) {
                        throw new OrderIllegalStateException(
                                "Cannot close order with unpaid order items.");
                    }
                });
        markOrderClosed(order);
    }

    private void markUnpaidOrderItem(
            OrderItem orderItem,
            OrderItem.OrderItemStatus statusToApply,
            CloseOrderManuallyRequest request) {
        if (FINAL_ORDER_ITEM_STATUSES.contains(orderItem.getStatus())) {
            return;
        }

        orderItem.setStatus(statusToApply);
        if (request.employeeComment() != null && !request.employeeComment().isBlank()) {
            orderItem.setEmployeeNote(request.employeeComment());
        }
    }

    private void assignEmployeeOnConfirmation(
            OrderItem orderItem,
            OrderItem.OrderItemStatus newStatus,
            Employee employee) {
        if (newStatus == OrderItem.OrderItemStatus.CONFIRMED
                && orderItem.getOrder().getEmployee() == null) {
            orderItem.getOrder().setEmployee(employee);
        }
    }

    private void markOrderClosed(Order order) {
        order.setClosedAt(LocalDateTime.now());
        order.setStatus(Order.OrderStatus.CLOSED);
    }
}
