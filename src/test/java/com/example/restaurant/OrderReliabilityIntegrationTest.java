package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.restaurant.dto.request.order.AddOrderItemRequest;
import com.example.restaurant.dto.request.order.UpdateOrderItemEmployeeRequest;
import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.order.OrderItem;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.support.AbstractIntegrationTest;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrderReliabilityIntegrationTest extends AbstractIntegrationTest {
    @Test
    void retriesReuseOriginalPositionEvenAfterEditingAndCancellation() {
        var f = fixture(Restaurant.Currency.UAH);
        var req = guestRequest(f);
        var body = new AddOrderItemRequest(2L, 1, "No sugar");
        guestOrders.addOrderItem(req, body);
        var added = items.findAllByOrderId(f.orderId()).stream()
                .filter(item -> !item.getId().equals(f.itemId())).findFirst().orElseThrow();
        transactions.executeWithoutResult(ignored -> {
            var item = items.findById(added.getId()).orElseThrow();
            item.setQuantity(3);
            item.setStatus(OrderItem.OrderItemStatus.CANCELED);
        });
        guestOrders.addOrderItem(req, body);
        assertThat(items.findAllByOrderId(f.orderId())).hasSize(2);
        assertThatThrownBy(() -> guestOrders.addOrderItem(req, new AddOrderItemRequest(2L, 2, "No sugar")))
                .hasMessageContaining("Idempotency key");
        assertThat(items.findById(added.getId()).orElseThrow().getQuantity()).isEqualTo(3);
    }

    @Test
    void concurrentRetriesCommitOnlyOnePosition() throws Exception {
        var f = fixture(Restaurant.Currency.UAH);
        String key = UUID.randomUUID().toString();
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Void> attempt = () -> {
                var req = guestRequest(f);
                req.removeHeader("Idempotency-Key");
                req.addHeader("Idempotency-Key", key);
                start.await();
                guestOrders.addOrderItem(req, new AddOrderItemRequest(2L, 1, null));
                return null;
            };
            var a = pool.submit(attempt);
            var b = pool.submit(attempt);
            start.countDown();
            a.get(10, TimeUnit.SECONDS);
            b.get(10, TimeUnit.SECONDS);
        }
        assertThat(items.findAllByOrderId(f.orderId())).hasSize(2);
    }

    @Test
    void keysAreCaseSensitiveAndScopedToGuest() {
        var f = fixture(Restaurant.Currency.UAH);
        for (String key : new String[]{"Meal", "meal"}) {
            var req = guestRequest(f);
            req.removeHeader("Idempotency-Key");
            req.addHeader("Idempotency-Key", key);
            guestOrders.addOrderItem(req, new AddOrderItemRequest(2L, 1, null));
        }
        var other = fixture(Restaurant.Currency.UAH);
        var req = guestRequest(other);
        req.removeHeader("Idempotency-Key");
        req.addHeader("Idempotency-Key", "Meal");
        guestOrders.addOrderItem(req, new AddOrderItemRequest(2L, 1, null));
        assertThat(items.findAllByOrderId(f.orderId())).hasSize(3);
        assertThat(items.findAllByOrderId(other.orderId())).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "bad key", "invalid/key"})
    void httpRejectsMissingOrInvalidKey(String key) throws Exception {
        var f = fixture(Restaurant.Currency.UAH);
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/orders/guest/items"))
                .header("Cookie", guestCookie(f)).header("Content-Type", "application/json");
        if (!key.isEmpty()) builder.header("Idempotency-Key", key);
        var request = builder.POST(HttpRequest.BodyPublishers.ofString(
                "{\"menuItemId\":2,\"quantity\":1}")).build();
        assertThat(HTTP.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(400);
        assertThat(items.count()).isEqualTo(1);
    }

    @Test
    void paidBeforeServingRetainsPaymentWhileDeliveryProgressesAndCannotCloseEarly() {
        var f = fixture(Restaurant.Currency.UAH);
        long payment = addOnlinePayment(f, Payment.PaymentStatus.PENDING);
        transactions.executeWithoutResult(ignored -> settlement.applyStatus(
                settlement.lockById(payment), Payment.PaymentStatus.SUCCEEDED, null));
        assertThat(items.findById(f.itemId()).orElseThrow().getFulfillmentStatus())
                .isEqualTo(OrderItem.FulfillmentStatus.CONFIRMED);
        assertThatThrownBy(() -> orderService.closeOrder(f.orderId())).hasMessageContaining("Serve");
        var served = orderService.updateOrderItem(f.itemId(), new UpdateOrderItemEmployeeRequest(null,
                UpdateOrderItemEmployeeRequest.OrderItemStatusDto.SERVED, null), employees.findById(2L).orElseThrow());
        assertThat(served.getFirst().orderItems().getFirst().fulfillmentStatus())
                .isEqualTo(OrderItem.FulfillmentStatus.SERVED);
        assertThat(items.findById(f.itemId()).orElseThrow().getStatus()).isEqualTo(OrderItem.OrderItemStatus.PAID_CARD);
        orderService.closeOrder(f.orderId());
        assertThat(payments.findById(payment).orElseThrow().getStatus()).isEqualTo(Payment.PaymentStatus.SUCCEEDED);
    }

    @Test
    void payingServedItemDoesNotLoseDeliveryState() {
        var f = fixture(Restaurant.Currency.UAH);
        setItemStatus(f, OrderItem.OrderItemStatus.SERVED);
        long id = addOnlinePayment(f, Payment.PaymentStatus.PENDING);
        transactions.executeWithoutResult(ignored -> settlement.applyStatus(
                settlement.lockById(id), Payment.PaymentStatus.SUCCEEDED, null));
        assertThat(items.findById(f.itemId()).orElseThrow().getFulfillmentStatus())
                .isEqualTo(OrderItem.FulfillmentStatus.SERVED);
    }
}
