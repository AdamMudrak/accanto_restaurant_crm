package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.restaurant.dto.request.order.AddOrderItemEmployeeRequest;
import com.example.restaurant.dto.request.order.UpdateOrderItemEmployeeRequest;
import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.order.Order;
import com.example.restaurant.entity.order.OrderItem;
import com.example.restaurant.exception.types.conflict.GuestIllegalStateException;
import com.example.restaurant.exception.types.conflict.ItemIllegalStateException;
import com.example.restaurant.exception.types.conflict.OrderIllegalStateException;
import com.example.restaurant.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;

class EmployeeOrderIntegrationTest extends AbstractIntegrationTest {
    @ParameterizedTest(name = "{0} -> {1}: allowed={2}")
    @CsvSource({
            "ORDERED,CONFIRMED,true", "ORDERED,CANCELED,true", "ORDERED,SERVED,false", "ORDERED,PAID_CASH,true",
            "CONFIRMED,CONFIRMED,true", "CONFIRMED,CANCELED,true", "CONFIRMED,SERVED,true", "CONFIRMED,PAID_CASH,true",
            "SERVED,CONFIRMED,false", "SERVED,CANCELED,false", "SERVED,SERVED,true", "SERVED,PAID_CASH,true",
            "CANCELED,CONFIRMED,false", "CANCELED,CANCELED,true", "CANCELED,SERVED,false", "CANCELED,PAID_CASH,false",
            "PAID_CASH,CONFIRMED,false", "PAID_CASH,CANCELED,false", "PAID_CASH,SERVED,true", "PAID_CASH,PAID_CASH,true",
            "PAID_CARD,CONFIRMED,false", "PAID_CARD,CANCELED,false", "PAID_CARD,SERVED,true", "PAID_CARD,PAID_CASH,false"
    })
    void employeeStatusTransitionMatrix(OrderItem.OrderItemStatus initial,
            UpdateOrderItemEmployeeRequest.OrderItemStatusDto target, boolean allowed) {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        setItemStatus(fixture, initial);
        var command = new UpdateOrderItemEmployeeRequest(null, target, null);
        if (allowed) {
            orderService.updateOrderItem(fixture.itemId(), command, employees.findById(2L).orElseThrow());
            assertThat(items.findById(fixture.itemId()).orElseThrow().getStatus().name())
                    .isEqualTo(initial.name().startsWith("PAID_") ? initial.name() : target.name());
        } else {
            assertThatThrownBy(() -> orderService.updateOrderItem(fixture.itemId(), command,
                    employees.findById(2L).orElseThrow())).isInstanceOf(ItemIllegalStateException.class);
            assertThat(items.findById(fixture.itemId()).orElseThrow().getStatus()).isEqualTo(initial);
        }
    }

    @Test
    void employeesCanAddForExistingGuestOrCreateNewGuestButCannotUseAnotherOrdersGuest() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        orderService.addOrderItem(fixture.orderId(), new AddOrderItemEmployeeRequest(
                fixture.guestId(), 2L, 1, "Prepare first"));
        orderService.addOrderItem(fixture.orderId(), new AddOrderItemEmployeeRequest(null, 2L, 1, null));
        assertThat(guests.count()).isEqualTo(2);
        assertThat(items.findAllByOrderId(fixture.orderId())).hasSize(3);
        Fixture other = fixture(Restaurant.Currency.UAH);
        assertThatThrownBy(() -> orderService.addOrderItem(fixture.orderId(),
                new AddOrderItemEmployeeRequest(other.guestId(), 2L, 1, null)))
                .isInstanceOf(GuestIllegalStateException.class);
    }

    @Test
    void quantityAndCommentsCanBeEditedBeforeConfirmationAndFirstConfirmationAssignsEmployee() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        setItemStatus(fixture, OrderItem.OrderItemStatus.ORDERED);
        jdbc.update("UPDATE orders SET employee_id = NULL WHERE id = ?", fixture.orderId());
        orderService.updateOrderItem(fixture.itemId(), new UpdateOrderItemEmployeeRequest(3,
                UpdateOrderItemEmployeeRequest.OrderItemStatusDto.CONFIRMED, "Confirmed"),
                employees.findById(2L).orElseThrow());
        assertThat(items.findById(fixture.itemId()).orElseThrow().getQuantity()).isEqualTo(3);
        assertThat(orders.findById(fixture.orderId()).orElseThrow().getEmployee().getId()).isEqualTo(2L);
        orderService.updateOrderItem(fixture.itemId(), new UpdateOrderItemEmployeeRequest(null,
                UpdateOrderItemEmployeeRequest.OrderItemStatusDto.SERVED, " "),
                employees.findById(3L).orElseThrow());
        assertThat(items.findById(fixture.itemId()).orElseThrow().getEmployeeNote()).isEqualTo("Confirmed");
        assertThat(orders.findById(fixture.orderId()).orElseThrow().getEmployee().getId()).isEqualTo(2L);
    }

    @ParameterizedTest
    @EnumSource(value = OrderItem.OrderItemStatus.class, names = "ORDERED", mode = EnumSource.Mode.EXCLUDE)
    void employeeCannotChangeQuantityAfterConfirmation(OrderItem.OrderItemStatus initial) {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        setItemStatus(fixture, initial);
        assertThatThrownBy(() -> orderService.updateOrderItem(fixture.itemId(),
                new UpdateOrderItemEmployeeRequest(3, null, null), employees.findById(2L).orElseThrow()))
                .isInstanceOf(ItemIllegalStateException.class);
    }

    @ParameterizedTest
    @EnumSource(value = OrderItem.OrderItemStatus.class, names = {"ORDERED", "CONFIRMED", "SERVED"})
    void normalCloseRejectsAnyUnsettledPosition(OrderItem.OrderItemStatus initial) {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        setItemStatus(fixture, initial);
        assertThatThrownBy(() -> orderService.closeOrder(fixture.orderId()))
                .isInstanceOf(OrderIllegalStateException.class);
        assertThat(orders.findById(fixture.orderId()).orElseThrow().getStatus()).isEqualTo(Order.OrderStatus.OPENED);
    }

    @ParameterizedTest
    @EnumSource(value = OrderItem.OrderItemStatus.class, names = {"PAID_CASH", "PAID_CARD", "CANCELED"})
    void normalCloseAcceptsSettledPositionsAndRecordsClosureTime(OrderItem.OrderItemStatus status) {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        setItemStatus(fixture, status);
        transactions.executeWithoutResult(ignored -> items.findAllByOrderId(fixture.orderId())
                .stream().filter(item -> item.getStatus().name().startsWith("PAID_"))
                .forEach(item -> item.setFulfillmentStatus(OrderItem.FulfillmentStatus.SERVED)));
        var closed = orderService.closeOrder(fixture.orderId());
        assertThat(closed.getOrderStatus()).isEqualTo(Order.OrderStatus.CLOSED);
        assertThat(closed.getClosedAt()).isNotNull();
    }

    @Test
    void manualCancellationDoesNotCreateRevenueAndRetainsReason() throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        var result = request("PUT", "/orders/" + fixture.orderId() + "/close-manually",
                Map.of("markUnpaidAs", "CANCELED", "employeeComment", "Guest left"), employeeCookie());
        assertThat(result.status()).isEqualTo(200);
        assertThat(items.findById(fixture.itemId()).orElseThrow().getStatus())
                .isEqualTo(OrderItem.OrderItemStatus.CANCELED);
        assertThat(items.findById(fixture.itemId()).orElseThrow().getEmployeeNote()).isEqualTo("Guest left");
        assertThat(payments.count()).isZero();
    }

    @Test
    void openOrderListDetailsAndHistoryFiltersReflectCompletedOrders() throws Exception {
        Fixture closed = fixture(Restaurant.Currency.UAH);
        closeCash(closed);
        Fixture open = fixture(Restaurant.Currency.UAH);
        assertThat(request("GET", "/orders", null, employeeCookie()).data().path("meta").path("total")
                .asLong()).isEqualTo(1);
        assertThat(request("GET", "/orders/" + open.orderId(), null, employeeCookie()).status()).isEqualTo(200);
        assertThat(request("GET", "/orders/" + closed.orderId(), null, employeeCookie()).status()).isEqualTo(404);
        assertThat(orderService.getOrderHistory(Order.OrderStatus.CLOSED,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                PageRequest.of(0, 10)).totalElements()).isEqualTo(1);
        assertThat(request("GET", "/orders/history?status=CLOSED", null, employeeCookie()).data()
                .path("meta").path("total").asLong()).isEqualTo(1);
        assertThat(request("GET", "/orders/" + closed.orderId() + "/payments", null, employeeCookie())
                .data().path("meta").path("total").asLong()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "deleted", "inactive"})
    void reassignmentRequiresAnActivatedExistingEmployee(String problem) throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        long target = problem.equals("missing") ? MISSING_ID : 3L;
        if (problem.equals("deleted")) {
            employeeService.deleteEmployee(3L);
        } else if (problem.equals("inactive")) {
            jdbc.update("UPDATE employees SET email_confirmed = false WHERE id = 3");
        }
        assertThat(request("PUT", "/orders/" + fixture.orderId() + "/employee",
                Map.of("employeeId", target), adminCookie()).status()).isEqualTo(409);
    }
}
