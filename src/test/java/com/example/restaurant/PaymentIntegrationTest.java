package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.restaurant.dto.request.order.UpdateOrderItemEmployeeRequest;
import com.example.restaurant.dto.request.payment.PaymentRequest;
import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.order.OrderItem;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.exception.types.conflict.ItemIllegalStateException;
import com.example.restaurant.exception.types.conflict.OrderIllegalStateException;
import com.example.restaurant.exception.types.notfound.OrderNotFoundException;
import com.example.restaurant.service.payment.PaymentStatusSyncScheduler;
import com.example.restaurant.service.payment.monobank.dto.request.MonobankInvoiceRequest;
import com.example.restaurant.service.payment.monobank.dto.response.MonobankInvoiceResponse;
import com.example.restaurant.service.payment.monobank.dto.response.MonobankStatusResponse;
import com.example.restaurant.service.payment.stripe.dto.request.StripeCheckoutSessionRequest;
import com.example.restaurant.service.payment.stripe.dto.response.StripeCheckoutSessionResponse;
import com.example.restaurant.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

class PaymentIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private PaymentStatusSyncScheduler scheduler;

    @Test
    void monobankCreationUsesSnapshottedPricesAndAddsTipsInMinorUnits() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        jdbc.update("UPDATE menu_items SET price = 999 WHERE id = 1");
        when(monobank.pay(any())).thenReturn(new MonobankInvoiceResponse("mono-created", "url", null));
        var result = paymentService.pay(new PaymentRequest(Set.of(fixture.itemId()),
                new BigDecimal("2.50")), guestRequest(fixture));
        assertThat(result.orderAmount()).isEqualByComparingTo("20.00");
        assertThat(result.tipAmount()).isEqualByComparingTo("2.50");
        assertThat(result.currency()).isEqualTo(Restaurant.Currency.UAH);
        var captor = ArgumentCaptor.forClass(
                MonobankInvoiceRequest.class);
        Mockito.verify(monobank).pay(captor.capture());
        assertThat(captor.getValue().amount()).isEqualTo(2250L);
        assertThat(captor.getValue().ccy()).isEqualTo(980);
        assertThat(captor.getValue().merchantPaymInfo().reference()).isEqualTo(result.paymentId().toString());
    }

    @Test
    void stripeCreationUsesOrderCurrencyAndReturnsProviderSession() {
        restaurantService.updateRestaurant(null, Restaurant.DefaultRegion.EUROPE, true, null, null);
        Fixture fixture = fixture(Restaurant.Currency.EUR);
        when(stripe.pay(any())).thenReturn(new StripeCheckoutSessionResponse(
                "cs_created", "https://example.com/pay", "open", "unpaid"));
        var result = paymentService.pay(new PaymentRequest(Set.of(fixture.itemId()), null), guestRequest(fixture));
        assertThat(result.invoiceId()).isEqualTo("cs_created");
        assertThat(result.pageUrl()).isEqualTo("https://example.com/pay");
        assertThat(result.tipAmount()).isEqualByComparingTo("0");
        var captor = ArgumentCaptor.forClass(
                StripeCheckoutSessionRequest.class);
        Mockito.verify(stripe).pay(captor.capture());
        assertThat(captor.getValue().currency()).isEqualTo("eur");
        assertThat(captor.getValue().amount()).isEqualTo(2000L);
    }

    @ParameterizedTest
    @EnumSource(value = OrderItem.OrderItemStatus.class,
            names = {"ORDERED", "CANCELED", "PAID_CASH", "PAID_CARD"})
    void onlyConfirmedOrServedPositionsCanBePaid(OrderItem.OrderItemStatus status) {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        setItemStatus(fixture, status);
        assertThatThrownBy(() -> paymentService.pay(new PaymentRequest(Set.of(fixture.itemId()), null),
                guestRequest(fixture))).isInstanceOf(ItemIllegalStateException.class);
        assertThat(payments.count()).isZero();
    }

    @Test
    void nullHistoricalOrderCurrencyBlocksNewOnlineAndCashReceipts() {
        Fixture fixture = fixture(null);
        assertThatThrownBy(() -> paymentService.pay(new PaymentRequest(Set.of(fixture.itemId()), null),
                guestRequest(fixture))).isInstanceOf(ItemIllegalStateException.class);
        assertThatThrownBy(() -> closeCash(fixture)).isInstanceOf(ItemIllegalStateException.class);
        assertThat(payments.count()).isZero();
    }

    @Test
    void selectedItemsMustBelongToOrderAndPayerMustBeItsGuest() throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Fixture other = fixture(Restaurant.Currency.UAH);
        assertThat(request("POST", "/payments", new PaymentRequest(Set.of(other.itemId()), null),
                guestCookie(fixture)).status()).isEqualTo(404);
        String forged = "orderUuid=" + fixture.orderUuid() + "; guestUuid=" + other.guestUuid();
        assertThat(request("POST", "/payments", new PaymentRequest(Set.of(fixture.itemId()), null), forged)
                .status()).isEqualTo(409);
    }

    @Test
    void selectionCannotMixDifferentPendingPaymentsOrExtendAnExistingInvoice() {
        Fixture first = fixture(Restaurant.Currency.UAH);
        Fixture second = additionalItem(first, OrderItem.OrderItemStatus.CONFIRMED);
        addOnlinePayment(first, Payment.PaymentStatus.PENDING);
        var request = new PaymentRequest(Set.of(first.itemId(), second.itemId()), null);
        assertThatThrownBy(() -> paymentService.pay(request, guestRequest(first)))
                .isInstanceOf(ItemIllegalStateException.class);
        addOnlinePayment(second, Payment.PaymentStatus.PENDING);
        assertThatThrownBy(() -> paymentService.pay(request, guestRequest(first)))
                .isInstanceOf(ItemIllegalStateException.class);
    }

    @ParameterizedTest
    @CsvSource({"created,PENDING", "processing,PENDING", "hold,PENDING",
            "success,SUCCEEDED", "failure,FAILED", "reversed,FAILED", "expired,EXPIRED"})
    void monobankSynchronizationMapsProviderStatesAndSettlesItems(String providerStatus, Payment.PaymentStatus expected) {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long id = addOnlinePayment(fixture, Payment.PaymentStatus.PENDING);
        var provider = MonobankStatusResponse.Status
                .valueOf(providerStatus);
        when(monobank.getStatus(any())).thenReturn(monoStatus(provider));
        var result = paymentService.syncPayment(id);
        assertThat(result.status()).isEqualTo(expected);
        assertThat(items.findById(fixture.itemId()).orElseThrow().getStatus())
                .isEqualTo(expected == Payment.PaymentStatus.SUCCEEDED
                        ? OrderItem.OrderItemStatus.PAID_CARD : OrderItem.OrderItemStatus.CONFIRMED);
    }

    @ParameterizedTest
    @CsvSource({"open,unpaid,PENDING", "complete,unpaid,PENDING",
            "complete,paid,SUCCEEDED", "expired,unpaid,EXPIRED"})
    void stripeSynchronizationMapsProviderStates(String state, String paid, Payment.PaymentStatus expected) {
        Fixture fixture = fixture(Restaurant.Currency.EUR);
        Long id = addOnlinePayment(fixture, Payment.PaymentStatus.PENDING);
        when(stripe.getStatus(any())).thenReturn(new StripeCheckoutSessionResponse(
                payments.findById(id).orElseThrow().getProviderPaymentId(), null, state, paid));
        assertThat(paymentService.syncPayment(id).status()).isEqualTo(expected);
    }

    @Test
    void synchronizationKeepsReservationAfterRepeatedErrorsAndRecoversOnLaterSuccess() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long id = addOnlinePayment(fixture, Payment.PaymentStatus.PENDING);
        when(monobank.getStatus(any())).thenThrow(new IllegalStateException("Provider unavailable"));
        for (int attempt = 0; attempt < 4; attempt++) {
            scheduler.syncPendingPayments();
        }
        assertThat(payments.findById(id).orElseThrow().getSyncAttempts()).isEqualTo(4);
        assertThat(paymentService.getPaymentById(id).lastSyncError()).contains("Provider unavailable");
        assertThatThrownBy(() -> closeCash(fixture)).isInstanceOf(ItemIllegalStateException.class);
        Mockito.doReturn(monoStatus(
                MonobankStatusResponse.Status.success))
                .when(monobank).getStatus(any());
        scheduler.syncPendingPayments();
        assertThat(payments.findById(id).orElseThrow().getSyncAttempts()).isZero();
        assertThat(paymentService.getPaymentById(id).lastSyncError()).isNull();
        assertThat(paymentService.getPaymentById(id).status()).isEqualTo(Payment.PaymentStatus.SUCCEEDED);
    }

    @Test
    void syncEndpointRejectsCashAndUnknownInvoicesButReportsUnresolvedProviderErrors() throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        closeCash(fixture);
        Long id = payments.findAll().getFirst().getId();
        assertThat(request("POST", "/payments/" + id + "/sync", null, employeeCookie()).status()).isEqualTo(409);
        assertThat(request("POST", "/payments/" + MISSING_ID + "/sync", null, employeeCookie()).status())
                .isEqualTo(404);
        assertThat(request("GET", "/payments/" + id, null, employeeCookie()).data().path("method").asText())
                .isEqualTo("CASH");
        assertThat(request("GET", "/payments?page=0&size=1", null, employeeCookie()).data().path("meta")
                .path("total").asLong()).isEqualTo(1);
    }

    @Test
    void manualCashCloseOnlyChargesRemainingUnpaidPositions() {
        Fixture online = fixture(Restaurant.Currency.UAH);
        addOnlinePayment(online, Payment.PaymentStatus.SUCCEEDED);
        Fixture cash = additionalItem(online, OrderItem.OrderItemStatus.SERVED);
        closeCash(cash);
        assertThat(payments.count()).isEqualTo(2);
        assertThat(payments.findAll().stream().filter(payment -> payment.getMethod() == Payment.PaymentMethod.CASH)
                .findFirst().orElseThrow().getAmount()).isEqualByComparingTo("20.00");
        assertThat(items.findById(online.itemId()).orElseThrow().getStatus()).isEqualTo(OrderItem.OrderItemStatus.PAID_CARD);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"orderItemsIds\":[]}",
            "{\"orderItemsIds\":[1],\"tipAmount\":-1}"})
    void paymentValidationRejectsEmptyItemsAndNegativeTips(String body) throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        assertThat(request("POST", "/payments", body, guestCookie(fixture)).status()).isEqualTo(400);
        assertThat(payments.count()).isZero();
    }

    private MonobankStatusResponse monoStatus(
            MonobankStatusResponse.Status status) {
        return new MonobankStatusResponse(
                status, null, null, null, OffsetDateTime.now());
    }
    @Test
    void cashCloseCreatesOneReceiptAndRepeatedCloseDoesNotDuplicateRevenue() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        closeCash(fixture);
        closeCash(fixture);
        assertThat(payments.count()).isEqualTo(1);
        Payment payment = payments.findAll().getFirst();
        assertThat(payment.getMethod()).isEqualTo(Payment.PaymentMethod.CASH);
        assertThat(payment.getCurrency()).isEqualTo(Restaurant.Currency.UAH);
        assertThat(payment.getAmount()).isEqualByComparingTo("20.00");
        assertThat(payment.getRecordedBy().getId()).isEqualTo(2L);
        assertThat(items.findById(fixture.itemId()).orElseThrow().getStatus())
                .isEqualTo(OrderItem.OrderItemStatus.PAID_CASH);
    }

    @Test
    void pendingOnlinePaymentBlocksCashCancellationAndManualCloseButAllowsServing() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        addOnlinePayment(fixture, Payment.PaymentStatus.PENDING);
        for (var status : List.of(UpdateOrderItemEmployeeRequest.OrderItemStatusDto.PAID_CASH,
                UpdateOrderItemEmployeeRequest.OrderItemStatusDto.CANCELED)) {
            assertThatThrownBy(() -> orderService.updateOrderItem(fixture.itemId(),
                    new UpdateOrderItemEmployeeRequest(null, status, null),
                    employees.findById(2L).orElseThrow()))
                    .isInstanceOf(ItemIllegalStateException.class);
        }
        assertThatThrownBy(() -> closeCash(fixture))
                .isInstanceOf(ItemIllegalStateException.class);
        orderService.updateOrderItem(fixture.itemId(), new UpdateOrderItemEmployeeRequest(null,
                UpdateOrderItemEmployeeRequest.OrderItemStatusDto.SERVED, "Ready"),
                employees.findById(2L).orElseThrow());
        assertThat(items.findById(fixture.itemId()).orElseThrow().getStatus())
                .isEqualTo(OrderItem.OrderItemStatus.SERVED);
    }

    @Test
    void confirmedFailureAllowsCashAndLateSuccessIsRecordedAsConflict() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long onlineId = addOnlinePayment(fixture, Payment.PaymentStatus.FAILED);
        closeCash(fixture);
        transactions.executeWithoutResult(ignored -> settlement.applyStatus(
                settlement.lockById(onlineId), Payment.PaymentStatus.SUCCEEDED,
                OffsetDateTime.now(ZoneOffset.UTC)));
        assertThat(payments.findById(onlineId).orElseThrow().getReconciliationIssue()).isNotBlank();
        assertThat(items.findById(fixture.itemId()).orElseThrow().getStatus())
                .isEqualTo(OrderItem.OrderItemStatus.PAID_CASH);
    }

    @Test
    void repeatedSuccessDoesNotChangeCompletionTime() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long id = addOnlinePayment(fixture, Payment.PaymentStatus.PENDING);
        OffsetDateTime paidAt = OffsetDateTime.parse("2030-01-31T23:59:00Z");
        transactions.executeWithoutResult(ignored -> settlement.applyStatus(
                settlement.lockById(id), Payment.PaymentStatus.SUCCEEDED, paidAt));
        transactions.executeWithoutResult(ignored -> settlement.applyStatus(
                settlement.lockById(id), Payment.PaymentStatus.SUCCEEDED, paidAt.plusDays(1)));
        assertThat(payments.findById(id).orElseThrow().getCompletedAt())
                .isEqualTo(paidAt.toLocalDateTime());
    }

    @Test
    void retryPreservesFailedAttemptAndRepeatedPendingRequestReusesInvoice() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long oldId = addOnlinePayment(fixture, Payment.PaymentStatus.FAILED);
        when(monobank.pay(any())).thenReturn(new MonobankInvoiceResponse("new-invoice", "url", null));
        PaymentRequest request = new PaymentRequest(Set.of(fixture.itemId()), BigDecimal.ZERO);
        var first = paymentService.pay(request, guestRequest(fixture));
        var repeated = paymentService.pay(request, guestRequest(fixture));
        assertThat(first.paymentId()).isNotEqualTo(oldId);
        assertThat(repeated.paymentId()).isEqualTo(first.paymentId());
        assertThat(payments.count()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_items WHERE payment_id = ?",
                Integer.class, oldId)).isEqualTo(1);
        transactions.executeWithoutResult(ignored -> settlement.applyStatus(
                settlement.lockById(oldId), Payment.PaymentStatus.SUCCEEDED,
                OffsetDateTime.now(ZoneOffset.UTC)));
        assertThat(payments.findById(oldId).orElseThrow().getReconciliationIssue()).isNotBlank();
        assertThat(items.findById(fixture.itemId()).orElseThrow().getPayment().getId())
                .isEqualTo(first.paymentId());
    }

    @Test
    void providerTimeoutKeepsReservationAndBlocksManualCash() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        when(monobank.pay(any())).thenThrow(new IllegalStateException("Simulated timeout"));
        assertThatThrownBy(() -> paymentService.pay(new PaymentRequest(
                Set.of(fixture.itemId()), BigDecimal.ZERO), guestRequest(fixture)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(payments.count()).isEqualTo(1);
        assertThat(payments.findAll().getFirst().getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
        assertThatThrownBy(() -> closeCash(fixture)).isInstanceOf(ItemIllegalStateException.class);
    }

    @Test
    void concurrentCashAndOnlinePaymentCannotBothReserveTheSameItems() throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        when(monobank.pay(any())).thenReturn(new MonobankInvoiceResponse("test-invoice", "url", null));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var cash = executor.submit(() -> {
                start.await();
                try {
                    closeCash(fixture);
                    return true;
                } catch (ItemIllegalStateException exception) {
                    return false;
                }
            });
            var online = executor.submit(() -> {
                start.await();
                try {
                    paymentService.pay(new PaymentRequest(Set.of(fixture.itemId()), BigDecimal.ZERO),
                            guestRequest(fixture));
                    return true;
                } catch (ItemIllegalStateException | OrderIllegalStateException exception) {
                    return false;
                } catch (OrderNotFoundException ex) {
                    return false;
                }
            });
            start.countDown();
            assertThat(List.of(cash.get(15, TimeUnit.SECONDS), online.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(payments.count()).isEqualTo(1);
    }

}
