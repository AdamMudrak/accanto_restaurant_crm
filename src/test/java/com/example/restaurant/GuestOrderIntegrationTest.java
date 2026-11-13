package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.restaurant.dto.request.order.AddOrderItemRequest;
import com.example.restaurant.dto.request.order.UpdateOrderItemRequest;
import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.order.Guest;
import com.example.restaurant.entity.order.OrderItem;
import com.example.restaurant.exception.types.conflict.ItemIllegalStateException;
import com.example.restaurant.exception.types.notfound.OrderItemNotFoundException;
import com.example.restaurant.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

class GuestOrderIntegrationTest extends AbstractIntegrationTest {
    @Test
    void guestCanOrderEditAndCancelOwnUnconfirmedPositionThroughApi() throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        var added = request("POST", "/orders/guest/items", new AddOrderItemRequest(2L, 1, "No sugar"),
                guestCookie(fixture));
        assertThat(added.status()).isEqualTo(200);
        var item = jdbc.queryForObject("SELECT id FROM order_items WHERE order_id = ? "
                + "AND menu_item_id = 2", Long.class, fixture.orderId());
        var edited = request("PUT", "/orders/guest/items/" + item,
                new UpdateOrderItemRequest(2, false, "Warm"), guestCookie(fixture));
        assertThat(edited.status()).isEqualTo(200);
        assertThat(items.findById(item).orElseThrow().getQuantity()).isEqualTo(2);
        assertThat(items.findById(item).orElseThrow().getClientNote()).isEqualTo("Warm");
        assertThat(request("PUT", "/orders/guest/items/" + item,
                new UpdateOrderItemRequest(null, true, null), guestCookie(fixture)).status())
                .isEqualTo(200);
        assertThat(items.findById(item).orElseThrow().getStatus())
                .isEqualTo(OrderItem.OrderItemStatus.CANCELED);
    }

    @Test
    void menuPriceAndNameAreSnapshottedWhenGuestOrders() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        guestOrders.addOrderItem(guestRequest(fixture), new AddOrderItemRequest(2L, 1, null));
        long id = jdbc.queryForObject("SELECT id FROM order_items WHERE order_id = ? "
                + "AND menu_item_id = 2", Long.class, fixture.orderId());
        BigDecimal oldPrice = items.findById(id).orElseThrow().getItemPriceSnapshot();
        String oldName = items.findById(id).orElseThrow().getItemNameSnapshot();
        jdbc.update("UPDATE menu_items SET price = 999, name_default_language = 'Changed' WHERE id = 2");
        var current = guestOrders.getCurrentOrder(guestRequest(fixture));
        var response = current.order().getOrderItemsByGuests().stream()
                .flatMap(group -> group.orderItems().stream()).filter(item -> item.id().equals(id))
                .findFirst().orElseThrow();
        assertThat(response.itemPriceSnapshot()).isEqualByComparingTo(oldPrice);
        assertThat(response.itemNameSnapshot()).isEqualTo(oldName);
    }

    @Test
    void perGuestQuantityLimitIncludesExistingItemsButExcludesCancelledItems() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        jdbc.update("UPDATE menu_items SET max_quantity_per_person = 3 WHERE id = 1");
        guestOrders.addOrderItem(guestRequest(fixture), new AddOrderItemRequest(1L, 1, null));
        assertThatThrownBy(() -> guestOrders.addOrderItem(guestRequest(fixture),
                new AddOrderItemRequest(1L, 1, null))).isInstanceOf(ItemIllegalStateException.class);
        setItemStatus(fixture, OrderItem.OrderItemStatus.CANCELED);
        guestOrders.addOrderItem(guestRequest(fixture), new AddOrderItemRequest(1L, 2, null));
        assertThat(items.findAllByOrderId(fixture.orderId())).hasSize(3);
    }

    @Test
    void quantityUpdateCountsOtherPositionsAndRetainsCommentForBlankUpdate() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        setItemStatus(fixture, OrderItem.OrderItemStatus.ORDERED);
        jdbc.update("UPDATE menu_items SET max_quantity_per_person = 4 WHERE id = 1");
        guestOrders.addOrderItem(guestRequest(fixture), new AddOrderItemRequest(1L, 2, null));
        assertThatThrownBy(() -> guestOrders.updateOrderItem(fixture.itemId(), guestRequest(fixture),
                new UpdateOrderItemRequest(3, false, null)))
                .isInstanceOf(ItemIllegalStateException.class);
        guestOrders.updateOrderItem(fixture.itemId(), guestRequest(fixture),
                new UpdateOrderItemRequest(1, false, "Keep this"));
        guestOrders.updateOrderItem(fixture.itemId(), guestRequest(fixture),
                new UpdateOrderItemRequest(null, false, " "));
        assertThat(items.findById(fixture.itemId()).orElseThrow().getQuantity()).isEqualTo(1);
        assertThat(items.findById(fixture.itemId()).orElseThrow().getClientNote()).isEqualTo("Keep this");
    }

    @ParameterizedTest
    @EnumSource(value = OrderItem.OrderItemStatus.class, names = "ORDERED", mode = EnumSource.Mode.EXCLUDE)
    void guestCannotEditConfirmedServedCancelledOrPaidItems(OrderItem.OrderItemStatus status) {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        setItemStatus(fixture, status);
        assertThatThrownBy(() -> guestOrders.updateOrderItem(fixture.itemId(), guestRequest(fixture),
                new UpdateOrderItemRequest(1, true, "Changed")))
                .isInstanceOf(ItemIllegalStateException.class);
        assertThat(items.findById(fixture.itemId()).orElseThrow().getStatus()).isEqualTo(status);
    }

    @Test
    void guestsCannotReadOrMutateAnotherOrderByCombiningCookies() throws Exception {
        Fixture owner = fixture(Restaurant.Currency.UAH);
        Fixture stranger = fixture(Restaurant.Currency.UAH);
        String forged = "guestUuid=" + stranger.guestUuid() + "; orderUuid=" + owner.orderUuid();
        assertThat(request("GET", "/orders/guest/current", null, forged).status()).isEqualTo(409);
        assertThat(request("POST", "/orders/guest/items", new AddOrderItemRequest(2L, 1, null), forged)
                .status()).isEqualTo(409);
        assertThat(request("PUT", "/orders/guest/items/" + owner.itemId(),
                new UpdateOrderItemRequest(1, false, null), forged).status()).isEqualTo(409);
    }

    @Test
    void guestCannotEditAnotherGuestsPositionAtSameTableAndSeesOwnGroupFirst() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Guest other = transactions.execute(ignored -> {
            Guest guest = new Guest();
            guest.setUuid(UUID.randomUUID().toString());
            guest.setOrder(orders.findById(fixture.orderId()).orElseThrow());
            return guests.save(guest);
        });
        var request = new MockHttpServletRequest();
        request.addHeader("Idempotency-Key", java.util.UUID.randomUUID().toString());
        request.setCookies(new jakarta.servlet.http.Cookie("guestUuid", other.getUuid()),
                new jakarta.servlet.http.Cookie("orderUuid", fixture.orderUuid()));
        guestOrders.addOrderItem(request, new AddOrderItemRequest(2L, 1, null));
        assertThat(guestOrders.getCurrentOrder(request).order().getOrderItemsByGuests().getFirst().guestId())
                .isEqualTo(other.getId());
        assertThat(guestOrders.getCurrentOrder(request).order().getOrderItemsByGuests()).hasSize(2);
        assertThatThrownBy(() -> guestOrders.updateOrderItem(fixture.itemId(), request,
                new UpdateOrderItemRequest(1, false, null)))
                .isInstanceOf(OrderItemNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing-cookie", "unknown-guest", "closed-order", "unavailable-dish", "unknown-dish"})
    void unavailableGuestOrderingResourcesReturnNotFound(String problem) throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        String cookie = guestCookie(fixture);
        long dishId = 2L;
        if (problem.equals("missing-cookie")) {
            cookie = null;
        } else if (problem.equals("unknown-guest")) {
            cookie = "guestUuid=" + UUID.randomUUID() + "; orderUuid=" + fixture.orderUuid();
        } else if (problem.equals("closed-order")) {
            closeCash(fixture);
        } else if (problem.equals("unavailable-dish")) {
            menuService.deleteMenuItem(2L);
        } else {
            dishId = MISSING_ID;
        }
        assertThat(request("POST", "/orders/guest/items", new AddOrderItemRequest(dishId, 1, null), cookie)
                .status()).isEqualTo(404);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"menuItemId\":1,\"quantity\":0}",
            "{\"menuItemId\":1,\"quantity\":-1}", "{\"quantity\":1}"})
    void invalidGuestItemRequestsDoNotCreatePositions(String body) throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        assertThat(request("POST", "/orders/guest/items", body, guestCookie(fixture)).status())
                .isEqualTo(400);
        assertThat(items.count()).isEqualTo(1);
    }
}
