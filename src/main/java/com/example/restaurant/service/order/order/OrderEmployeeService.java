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
import com.example.restaurant.entity.payment.PaymentRefund;
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
import com.example.restaurant.repository.management.RestaurantRepository;
import com.example.restaurant.repository.menu.MenuItemRepository;
import com.example.restaurant.repository.order.GuestRepository;
import com.example.restaurant.repository.order.OrderItemRepository;
import com.example.restaurant.repository.order.OrderRepository;
import com.example.restaurant.repository.user.EmployeeRepository;
import com.example.restaurant.service.payment.CashPaymentService;
import com.example.restaurant.service.payment.PaymentSettlementService;
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
    private final CashPaymentService cashPaymentService;
    private final PaymentSettlementService settlementService;
    private final RestaurantRepository restaurantRepository;
    private final EmployeeRepository employeeRepository;

    public OrderResponse reassignEmployee(Long orderId, Long employeeId) {
        restaurantRepository.lockSingleton().orElseThrow();
        Order order = findOrderById(orderId);
        Employee employee = employeeRepository.findById(employeeId)
                .filter(candidate -> !candidate.isDeleted() && candidate.isEmailConfirmed())
                .orElseThrow(() -> new OrderIllegalStateException("Active employee required."));
        order.setEmployee(employee);
        return orderMapper.toOrderResponse(order);
    }

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
        Order order = orderRepository.findByIdAndStatus(orderId, Order.OrderStatus.OPENED)
                .orElseThrow(() -> new OrderNotFoundException("Open order not found"));

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
        restaurantRepository.lockSingleton().orElseThrow();
        Employee currentEmployee = employeeRepository.findById(employee.getId()).orElseThrow();
        if (currentEmployee.isDeleted()) {
            throw new OrderIllegalStateException("Deleted employees cannot manage orders.");
        }
        Long orderId = orderItemRepository.findOrderIdByItemId(orderItemId)
                .orElseThrow(() -> new OrderItemNotFoundException("Order item not found"));
        findOrderById(orderId);
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

    public OrderResponse closeOrderManually(Long id, CloseOrderManuallyRequest request,
                                           Employee employee) {
        Order order = orderRepository.lockById(id).orElseThrow(() ->
                new OrderNotFoundException("Order not found"));
        if (order.getStatus() == Order.OrderStatus.CLOSED) {
            return orderMapper.toOrderResponse(order);
        }
        settlementService.assertNoPendingPayments(order);
        OrderItem.OrderItemStatus statusToApply =
                OrderItem.OrderItemStatus.valueOf(request.markUnpaidAs().name());

        List<OrderItem> items = orderItemRepository.findAllByOrderId(order.getId());
        if (request.employeeComment() != null && !request.employeeComment().isBlank()) {
            items.stream().filter(item -> !FINAL_ORDER_ITEM_STATUSES.contains(item.getStatus()))
                    .forEach(item -> item.setEmployeeNote(request.employeeComment()));
        }
        if (statusToApply == OrderItem.OrderItemStatus.PAID_CASH) {
            cashPaymentService.record(items, employee);
        }
        items.forEach(orderItem -> markUnpaidOrderItem(orderItem, statusToApply, request));
        markOrderClosed(order);

        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    private Order findOrderById(Long orderId) {
        return orderRepository
                .lockById(orderId)
                .filter(order -> order.getStatus() == Order.OrderStatus.OPENED)
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
        if ((orderItem.getStatus() == OrderItem.OrderItemStatus.PAID_CARD
                || orderItem.getStatus() == OrderItem.OrderItemStatus.PAID_CASH)
                && newStatus == OrderItem.OrderItemStatus.SERVED) {
            orderItem.setFulfillmentStatus(OrderItem.FulfillmentStatus.SERVED);
            return;
        }
        if ((orderItem.getStatus() == OrderItem.OrderItemStatus.PAID_CARD
                || orderItem.getStatus() == OrderItem.OrderItemStatus.PAID_CASH)
                && newStatus == OrderItem.OrderItemStatus.CANCELED
                && orderItem.getPayment() != null && orderItem.getPayment().getRefund() != null
                && orderItem.getPayment().getRefund().getStatus()
                == PaymentRefund.Status.SUCCEEDED) {
            orderItem.setFulfillmentStatus(OrderItem.FulfillmentStatus.CANCELED);
            return;
        }
        validateOrderItemStatusTransition(orderItem.getStatus(), newStatus);
        if (newStatus == OrderItem.OrderItemStatus.CANCELED
                || newStatus == OrderItem.OrderItemStatus.PAID_CASH) {
            settlementService.assertNoPendingPayment(orderItem);
        }
        if (newStatus == OrderItem.OrderItemStatus.PAID_CASH) {
            cashPaymentService.record(List.of(orderItem), employee);
            return;
        }
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
        settlementService.assertNoPendingPayments(order);
        orderItemRepository.findAllByOrderId(order.getId())
                .forEach(orderItem -> {
                    if (orderItem.getStatus() != OrderItem.OrderItemStatus.CANCELED
                            && orderItem.getFulfillmentStatus()
                            != OrderItem.FulfillmentStatus.SERVED
                            && orderItem.getFulfillmentStatus()
                            != OrderItem.FulfillmentStatus.CANCELED) {
                        throw new OrderIllegalStateException("Serve paid items before closing.");
                    }
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
