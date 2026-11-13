package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.restaurant.dto.request.order.UpdateOrderItemEmployeeRequest;
import com.example.restaurant.dto.request.payment.LinkInvoiceRequest;
import com.example.restaurant.dto.request.payment.RefundRequest;
import com.example.restaurant.dto.request.payment.ResolvePaymentRequest;
import com.example.restaurant.dto.response.payment.PaymentResponse;
import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.entity.payment.PaymentRefund;
import com.example.restaurant.exception.types.conflict.ItemIllegalStateException;
import com.example.restaurant.repository.payment.PaymentRefundRepository;
import com.example.restaurant.service.payment.PaymentOperationsService;
import com.example.restaurant.service.payment.ProviderInvoice;
import com.example.restaurant.service.payment.ProviderRefund;
import com.example.restaurant.support.AbstractIntegrationTest;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class PaymentOperationsIntegrationTest extends AbstractIntegrationTest {
    @Autowired PaymentOperationsService operations;
    @Autowired PaymentRefundRepository refunds;

    long unknownInvoice() {
        long id = addOnlinePayment(fixture(Restaurant.Currency.UAH), Payment.PaymentStatus.PENDING);
        jdbc.update("UPDATE payments SET provider_payment_id = NULL, reconciliation_issue = 'Timeout' WHERE id = ?", id);
        return id;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void lateRefundPollingCannotDowngradeFinalStateOrOverwriteNewAttempt(boolean retry) throws Exception {
        long id = addOnlinePayment(fixture(Restaurant.Currency.UAH), Payment.PaymentStatus.SUCCEEDED);
        when(monobank.refund(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new ProviderRefund("old", PaymentRefund.Status.PENDING, 2200, "UAH"))
                .thenReturn(new ProviderRefund("new", PaymentRefund.Status.PENDING, 2200, "UAH"));
        operations.refund(id, new RefundRequest("Return", null), employees.findById(1L).orElseThrow());
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        when(monobank.inspectRefund(anyString(), anyString(), anyString(), anyLong(), anyString()))
                .thenAnswer(invocation -> {
                    if (calls.getAndIncrement() == 0) {
                        entered.countDown();
                        if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
                        return new ProviderRefund("old", retry ? PaymentRefund.Status.SUCCEEDED : PaymentRefund.Status.PENDING, 2200, "UAH");
                    }
                    return new ProviderRefund("old", PaymentRefund.Status.FAILED, 2200, "UAH");
                });
        try (var worker = Executors.newSingleThreadExecutor()) {
            var slow = worker.submit(() -> operations.syncRefund(id, null));
            try {
                assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
                operations.syncRefund(id, null);
                if (retry) operations.retryRefund(id);
            } finally {
                release.countDown();
            }
            slow.get(10, TimeUnit.SECONDS);
        }
        var result = operations.getRefund(id);
        assertThat(result.status()).isEqualTo(retry ? PaymentRefund.Status.PENDING : PaymentRefund.Status.FAILED);
        assertThat(result.providerRefundId()).isEqualTo(retry ? "new" : "old");
        if (retry) assertThat(payments.findById(id).orElseThrow().getReconciliationIssue()).contains("previous refund attempt");
    }

    @Test
    void returnedUnservedDishCanBeCanceledAndVisitClosedWithoutInventingService() {
        var f = fixture(Restaurant.Currency.UAH);
        long id = addOnlinePayment(f, Payment.PaymentStatus.PENDING);
        transactions.executeWithoutResult(ignored -> settlement.applyStatus(settlement.lockById(id), Payment.PaymentStatus.SUCCEEDED, null));
        var update = new UpdateOrderItemEmployeeRequest(null,
                UpdateOrderItemEmployeeRequest.OrderItemStatusDto.CANCELED, null);
        var actor = employees.findById(1L).orElseThrow();
        assertThatThrownBy(() -> orderService.updateOrderItem(f.itemId(), update, actor)).hasMessageContaining("final");
        when(monobank.refund(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new ProviderRefund("returned", PaymentRefund.Status.SUCCEEDED, 2200, "UAH"));
        operations.refund(id, new RefundRequest("Dish canceled", null), actor);
        orderService.updateOrderItem(f.itemId(), update, actor);
        assertThat(items.findById(f.itemId()).orElseThrow().getFulfillmentStatus().name()).isEqualTo("CANCELED");
        orderService.closeOrder(f.orderId());
        assertThat(payments.findById(id).orElseThrow().getStatus()).isEqualTo(Payment.PaymentStatus.SUCCEEDED);
    }

    @Test
    void reconciliationQueueIncludesUnknownInvoicesAndFailedRefundsWithoutNormalReceipts() throws Exception {
        long unknown = unknownInvoice();
        long normal = addOnlinePayment(fixture(Restaurant.Currency.UAH), Payment.PaymentStatus.SUCCEEDED);
        var response = request("GET", "/payments/reconciliation", null, adminCookie());
        assertThat(response.status()).isEqualTo(200);
        var listed = paymentService.getPaymentsNeedingReconciliation(PageRequest.of(0, 10));
        assertThat(listed.items()).extracting(PaymentResponse::paymentId).containsExactly(unknown);
        assertThat(request("GET", "/payments/reconciliation", null, employeeCookie()).status()).isEqualTo(403);
        when(monobank.refund(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new ProviderRefund("failed", PaymentRefund.Status.FAILED, 2200, "UAH"));
        operations.refund(normal, new RefundRequest("Return", null), employees.findById(1L).orElseThrow());
        assertThat(paymentService.getPaymentsNeedingReconciliation(PageRequest.of(0, 10)).items())
                .extracting(PaymentResponse::paymentId).containsExactlyInAnyOrder(unknown, normal);
    }

    @Test
    void onlyConfirmedFailedRefundMayBeRetriedWithNewProviderReference() {
        long id = addOnlinePayment(fixture(Restaurant.Currency.UAH), Payment.PaymentStatus.SUCCEEDED);
        when(monobank.refund(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new ProviderRefund("failed", PaymentRefund.Status.FAILED, 2200, "UAH"))
                .thenReturn(new ProviderRefund("retry", PaymentRefund.Status.SUCCEEDED, 2200, "UAH"));
        operations.refund(id, new RefundRequest("Return", null), employees.findById(1L).orElseThrow());
        assertThat(operations.retryRefund(id).status()).isEqualTo(PaymentRefund.Status.SUCCEEDED);
        var keys = ArgumentCaptor.forClass(String.class);
        verify(monobank, times(2)).refund(anyString(), keys.capture(), eq(2200L), eq("UAH"));
        assertThat(keys.getAllValues()).doesNotHaveDuplicates();
        assertThatThrownBy(() -> operations.retryRefund(id)).hasMessageContaining("provider-confirmed");
    }

    @Test
    void concurrentRefundRequestsInvokeProviderOnce() throws Exception {
        long id = addOnlinePayment(fixture(Restaurant.Currency.UAH), Payment.PaymentStatus.SUCCEEDED);
        when(monobank.refund(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new ProviderRefund("once", PaymentRefund.Status.SUCCEEDED, 2200, "UAH"));
        var actor = employees.findById(1L).orElseThrow();
        var latch = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Object> attempt = () -> { latch.await(); return operations.refund(id, new RefundRequest("Return", null), actor); };
            var a = pool.submit(attempt); var b = pool.submit(attempt); latch.countDown();
            a.get(10, TimeUnit.SECONDS); b.get(10, TimeUnit.SECONDS);
        }
        assertThat(refunds.count()).isEqualTo(1);
        verify(monobank, times(1)).refund(anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void verifiesAndLinksLostInvoiceThenSettlesWithoutSqlRepair() throws Exception {
        long id = unknownInvoice();
        when(monobank.inspect("lost")).thenReturn(new ProviderInvoice("lost", "" + id, 2200, "UAH", Payment.PaymentStatus.SUCCEEDED, null));
        var response = request("POST", "/payments/" + id + "/invoice", Map.of("invoiceId", "lost"), adminCookie());
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.data().path("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(response.data().path("reconciliationIssue").isNull()).isTrue();
        assertThat(payments.findById(id).orElseThrow().getResolvedAt()).isNotNull();
        assertThat(items.findAll().getFirst().getStatus().name()).isEqualTo("PAID_CARD");
    }

    @ParameterizedTest
    @ValueSource(strings = {"reference", "amount", "currency", "id"})
    void mismatchedProviderInvoiceCannotBeLinked(String field) {
        long id = unknownInvoice();
        when(monobank.inspect("lost")).thenReturn(new ProviderInvoice(field.equals("id") ? "other" : "lost",
                field.equals("reference") ? "other" : "" + id, field.equals("amount") ? 1 : 2200,
                field.equals("currency") ? "EUR" : "UAH", Payment.PaymentStatus.SUCCEEDED, null));
        assertThatThrownBy(() -> operations.link(id, new LinkInvoiceRequest("lost"), employees.findById(1L).orElseThrow()))
                .hasMessageContaining("does not match");
        assertThat(payments.findById(id).orElseThrow().getProviderPaymentId()).isNull();
        assertThat(payments.findById(id).orElseThrow().getReconciliationIssue()).isNotNull();
    }

    @Test
    void failedInvoiceCanBeLinkedAndReleasesReservation() {
        long id = unknownInvoice();
        when(monobank.inspect("failed")).thenReturn(new ProviderInvoice("failed", "" + id, 2200, "UAH", Payment.PaymentStatus.FAILED, null));
        operations.link(id, new LinkInvoiceRequest("failed"), employees.findById(1L).orElseThrow());
        assertThat(payments.findById(id).orElseThrow().getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
        assertThat(payments.findById(id).orElseThrow().getReconciliationIssue()).isNull();
    }

    @Test
    void unknownInvoiceRequiresExplicitDashboardEvidenceBeforeReleasingReservation() throws Exception {
        long id = unknownInvoice();
        assertThat(request("POST", "/payments/" + id + "/resolution", Map.of("action", "CONFIRM_NOT_CHARGED", "evidence", ""), adminCookie()).status()).isEqualTo(400);
        assertThat(request("POST", "/payments/" + id + "/resolution", Map.of("action", "CONFIRM_NOT_CHARGED", "evidence", "Merchant checked local reference; no invoice or charge"), adminCookie()).status()).isEqualTo(200);
        assertThat(payments.findById(id).orElseThrow().getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
        assertThat(payments.findById(id).orElseThrow().getResolutionNote()).contains("Merchant checked");
    }

    @Test
    void knownPendingInvoiceCannotBeManuallyDeclaredUncharged() {
        long id = addOnlinePayment(fixture(Restaurant.Currency.UAH), Payment.PaymentStatus.PENDING);
        jdbc.update("UPDATE payments SET reconciliation_issue = 'Check' WHERE id = ?", id);
        assertThatThrownBy(() -> operations.resolve(id, new ResolvePaymentRequest(ResolvePaymentRequest.Action.CONFIRM_NOT_CHARGED, "Checked"), employees.findById(1L).orElseThrow()))
                .hasMessageContaining("Only an unknown");
    }

    @Test
    void fullRefundIsIdempotentKeepsGrossReceiptAndSeparatelyReportsReturnedRevenueAndTips() throws Exception {
        var f = fixture(Restaurant.Currency.UAH);
        long id = addOnlinePayment(f, Payment.PaymentStatus.SUCCEEDED);
        when(monobank.refund(anyString(), anyString(), eq(2200L), eq("UAH")))
                .thenReturn(new ProviderRefund("refund1", PaymentRefund.Status.SUCCEEDED, 2200, "UAH"));
        String path = "/payments/" + id + "/refund";
        var result = request("POST", path, Map.of("reason", "Customer requested"), adminCookie());
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.data().path("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(request("POST", path, Map.of("reason", "Retried request"), adminCookie()).status()).isEqualTo(200);
        verify(monobank, times(1)).refund(anyString(), anyString(), eq(2200L), eq("UAH"));
        assertThat(refunds.count()).isEqualTo(1);
        assertThat(payments.findById(id).orElseThrow().getStatus()).isEqualTo(Payment.PaymentStatus.SUCCEEDED);
        assertThatThrownBy(() -> analytics.markTipPaid(id)).hasMessageContaining("cannot be paid");
        var month = YearMonth.now(ZoneId.of("Europe/Kyiv"));
        var report = analytics.getRevenueSummary(month.getYear(), month.getMonthValue());
        assertThat(report.currencies().getFirst().totalRevenue()).isEqualByComparingTo("20.00");
        assertThat(report.refunds().getFirst().refundedRevenue()).isEqualByComparingTo("20.00");
        assertThat(report.refunds().getFirst().refundedTips()).isEqualByComparingTo("2.00");
        assertThat(analytics.getEmployeeTips(month.getYear(), month.getMonthValue())).isEmpty();
        assertThat(request("GET", "/reviews/eligibility?paymentId=" + id, null, guestCookie(f)).status()).isEqualTo(409);
    }

    @Test
    void ambiguousRefundStaysUnknownAndSyncRecoversWithoutSendingAnotherRefund() {
        long id = addOnlinePayment(fixture(Restaurant.Currency.EUR), Payment.PaymentStatus.SUCCEEDED);
        when(stripe.refund(anyString(), anyString(), anyLong(), anyString())).thenThrow(new IllegalStateException("Timeout"));
        var actor = employees.findById(1L).orElseThrow();
        assertThat(operations.refund(id, new RefundRequest("Return", null), actor).status()).isEqualTo(PaymentRefund.Status.UNKNOWN);
        operations.refund(id, new RefundRequest("Retry", null), actor);
        when(stripe.inspectRefund(anyString(), anyString(), eq("re_recovered"), eq(2200L), eq("EUR")))
                .thenReturn(new ProviderRefund("re_recovered", PaymentRefund.Status.SUCCEEDED, 2200, "EUR"));
        var result = operations.syncRefund(id, "re_recovered");
        assertThat(result.status()).isEqualTo(PaymentRefund.Status.SUCCEEDED);
        assertThat(result.completedAt()).isNotNull();
        assertThat(result.error()).isNull();
        verify(stripe, times(1)).refund(anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void pendingRefundIsPolledToCompletionAndExpectedReversalDoesNotFlagReceipt() {
        long id = addOnlinePayment(fixture(Restaurant.Currency.UAH), Payment.PaymentStatus.SUCCEEDED);
        when(monobank.refund(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new ProviderRefund("pending", PaymentRefund.Status.PENDING, 2200, "UAH"));
        operations.refund(id, new RefundRequest("Return", null), employees.findById(1L).orElseThrow());
        when(monobank.inspectRefund(anyString(), anyString(), eq("pending"), eq(2200L), eq("UAH")))
                .thenReturn(new ProviderRefund("pending", PaymentRefund.Status.SUCCEEDED, 2200, "UAH"));
        operations.syncPendingRefunds();
        assertThat(operations.getRefund(id).status()).isEqualTo(PaymentRefund.Status.SUCCEEDED);
        transactions.executeWithoutResult(ignored -> settlement.applyStatus(settlement.lockById(id), Payment.PaymentStatus.FAILED, null));
        assertThat(payments.findById(id).orElseThrow().getReconciliationIssue()).isNull();
    }

    @Test
    void refundMismatchIsNotCountedAsSuccessfulAndNoAutomaticRetryIsIssued() {
        long id = addOnlinePayment(fixture(Restaurant.Currency.UAH), Payment.PaymentStatus.SUCCEEDED);
        when(monobank.refund(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new ProviderRefund("wrong", PaymentRefund.Status.SUCCEEDED, 1, "UAH"));
        assertThat(operations.refund(id, new RefundRequest("Return", null), employees.findById(1L).orElseThrow()).status())
                .isEqualTo(PaymentRefund.Status.UNKNOWN);
        assertThat(operations.getRefund(id).completedAt()).isNull();
    }

    @Test
    void cashRefundRequiresExplicitPhysicalReturnConfirmation() {
        var f = fixture(Restaurant.Currency.UAH);
        closeCash(f);
        long id = payments.findAll().getFirst().getId();
        var actor = employees.findById(1L).orElseThrow();
        assertThatThrownBy(() -> operations.refund(id, new RefundRequest("Cash returned", false), actor))
                .hasMessageContaining("Confirm cash");
        assertThat(operations.refund(id, new RefundRequest("Cash returned", true), actor).status())
                .isEqualTo(PaymentRefund.Status.SUCCEEDED);
        verify(monobank, never()).refund(anyString(), anyString(), anyLong(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "FAILED", "EXPIRED", "RECONSTRUCTED", "TIP_PAID", "UNKNOWN_CURRENCY"})
    void refundRejectsIneligibleReceipts(String kind) {
        long id = addOnlinePayment(fixture(kind.equals("UNKNOWN_CURRENCY") ? null : Restaurant.Currency.UAH),
                Set.of("PENDING", "FAILED", "EXPIRED").contains(kind) ? Payment.PaymentStatus.valueOf(kind) : Payment.PaymentStatus.SUCCEEDED);
        if (kind.equals("RECONSTRUCTED")) jdbc.update("UPDATE payments SET reconstructed = true WHERE id = ?", id);
        if (kind.equals("TIP_PAID")) jdbc.update("UPDATE payments SET tip_paid = true WHERE id = ?", id);
        assertThatThrownBy(() -> operations.refund(id, new RefundRequest("Return", null), employees.findById(1L).orElseThrow()))
                .isInstanceOf(ItemIllegalStateException.class);
        assertThat(refunds.count()).isZero();
    }

    @Test
    void onlyAdministratorsCanOperatePaymentsAndRefunds() throws Exception {
        long id = unknownInvoice();
        for (String cookie : new String[]{null, employeeCookie()}) {
            assertThat(request("POST", "/payments/" + id + "/invoice", Map.of("invoiceId", "lost"), cookie).status()).isEqualTo(403);
            assertThat(request("POST", "/payments/" + id + "/resolution", Map.of("action", "CONFIRM_NOT_CHARGED", "evidence", "Checked"), cookie).status()).isEqualTo(403);
            assertThat(request("POST", "/payments/" + id + "/refund", Map.of("reason", "Return"), cookie).status()).isEqualTo(403);
            assertThat(request("POST", "/payments/" + id + "/refund/sync", null, cookie).status()).isEqualTo(403);
            assertThat(request("GET", "/payments/" + id + "/refund", null, cookie).status()).isEqualTo(403);
            assertThat(request("POST", "/payments/" + id + "/refund/retry", null, cookie).status()).isEqualTo(403);
        }
    }

    @Test
    void financialIssueCanOnlyBeAcknowledgedAfterRefundAndPreservesEvidence() {
        long id = addOnlinePayment(fixture(Restaurant.Currency.UAH), Payment.PaymentStatus.SUCCEEDED);
        jdbc.update("UPDATE payments SET reconciliation_issue = 'Duplicate charge' WHERE id = ?", id);
        var actor = employees.findById(1L).orElseThrow();
        var command = new ResolvePaymentRequest(ResolvePaymentRequest.Action.ACKNOWLEDGE, "Duplicate refunded");
        assertThatThrownBy(() -> operations.resolve(id, command, actor)).hasMessageContaining("confirmed refund");
        when(monobank.refund(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new ProviderRefund("refund", PaymentRefund.Status.SUCCEEDED, 2200, "UAH"));
        operations.refund(id, new RefundRequest("Duplicate", null), actor);
        assertThat(operations.resolve(id, command, actor).reconciliationIssue()).isNull();
        assertThat(payments.findById(id).orElseThrow().getResolutionNote()).contains("Duplicate refunded");
    }
}
