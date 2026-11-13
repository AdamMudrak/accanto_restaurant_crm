package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.order.OrderItem;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnalyticsIntegrationTest extends AbstractIntegrationTest {
    @Test
    void emptyMonthReportsZerosAndNoCurrencyBuckets() {
        var summary = analytics.getRevenueSummary(2030, 1);
        assertThat(summary.currencies()).isEmpty();
        assertThat(summary.unresolvedHistoricalPaymentsCount()).isZero();
        assertThat(analytics.getMonthlySummary(2030, 1, null).revenueAmount()).isEqualByComparingTo("0");
        assertThat(analytics.getEmployeeTips(2030, 1)).isEmpty();
    }

    @Test
    void monthlyBoundariesUseCompletionTimeAndExcludePendingFailedAndExpiredReceipts() {
        var now = LocalDateTime.of(2030, 1, 1, 0, 0)
                .atZone(java.time.ZoneId.of("Europe/Kyiv"))
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        for (Payment.PaymentStatus status : Payment.PaymentStatus.values()) {
            Long id = addOnlinePayment(fixture(Restaurant.Currency.UAH), status);
            jdbc.update("UPDATE payments SET completed_at = ? WHERE id = ?", now, id);
        }
        Long nextMonth = addOnlinePayment(fixture(Restaurant.Currency.UAH), Payment.PaymentStatus.SUCCEEDED);
        jdbc.update("UPDATE payments SET completed_at = ? WHERE id = ?", now.plusMonths(1), nextMonth);
        Long previous = addOnlinePayment(fixture(Restaurant.Currency.UAH), Payment.PaymentStatus.SUCCEEDED);
        jdbc.update("UPDATE payments SET completed_at = ? WHERE id = ?", now.minusSeconds(1), previous);
        var report = analytics.getMonthlySummary(2030, 1, Restaurant.Currency.UAH);
        assertThat(report.succeededPaymentsCount()).isEqualTo(1);
        assertThat(report.revenueAmount()).isEqualByComparingTo("20.00");
        assertThat(report.tipAmount()).isEqualByComparingTo("2.00");
        assertThat(analytics.getRevenueSummary(2030, 1).currencies().getFirst().orderCount()).isEqualTo(1);
    }

    @Test
    void severalPaymentsForOneOrderCountAsSeveralReceiptsAndOneOrder() {
        Fixture first = fixture(Restaurant.Currency.UAH);
        addOnlinePayment(first, Payment.PaymentStatus.SUCCEEDED);
        Fixture second = additionalItem(first, OrderItem.OrderItemStatus.CONFIRMED);
        closeCash(second);
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        var bucket = analytics.getRevenueSummary(now.getYear(), now.getMonthValue()).currencies().getFirst();
        assertThat(bucket.paymentCount()).isEqualTo(2);
        assertThat(bucket.orderCount()).isEqualTo(1);
        assertThat(bucket.totalRevenue()).isEqualByComparingTo("40.00");
        assertThat(bucket.onlineTips()).isEqualByComparingTo("2.00");
    }

    @Test
    void legacyUnknownCurrencyAndPaymentTimeAreVisibleWithoutBeingAssignedToKnownCurrency() {
        Fixture unknownCurrency = fixture(null);
        addOnlinePayment(unknownCurrency, Payment.PaymentStatus.SUCCEEDED);
        Long unknownTime = addOnlinePayment(fixture(Restaurant.Currency.UAH), Payment.PaymentStatus.SUCCEEDED);
        jdbc.update("UPDATE payments SET completed_at = NULL, reconstructed = true, "
                + "reconciliation_issue = 'Verify legacy data' WHERE id = ?", unknownTime);
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        var report = analytics.getRevenueSummary(now.getYear(), now.getMonthValue());
        assertThat(report.unresolvedHistoricalPaymentsCount()).isEqualTo(2);
        assertThat(report.reconciliationRequiredPaymentsCount()).isEqualTo(1);
        assertThat(report.currencies()).hasSize(1);
        assertThat(report.currencies().getFirst().currency()).isNull();
        assertThat(analytics.getMonthlySummary(now.getYear(), now.getMonthValue(), Restaurant.Currency.UAH)
                .succeededPaymentsCount()).isZero();
    }

    @Test
    void tipPayoutCanBeMarkedAndReversedAndFilteredThroughAdminApi() throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long id = addOnlinePayment(fixture, Payment.PaymentStatus.SUCCEEDED);
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        String filter = "?year=" + now.getYear() + "&month=" + now.getMonthValue();
        assertThat(request("GET", "/admin/analytics/tips/payments" + filter + "&tipPaid=false", null,
                adminCookie()).data().path("meta").path("total").asLong()).isEqualTo(1);
        var paid = request("PUT", "/admin/analytics/tips/payments/" + id + "/paid", null, adminCookie());
        assertThat(paid.status()).isEqualTo(200);
        assertThat(paid.data().path("tipPaid").asBoolean()).isTrue();
        assertThat(paid.data().path("tipPaidAt").isNull()).isFalse();
        assertThat(request("GET", "/admin/analytics/tips/payments" + filter + "&tipPaid=false", null,
                adminCookie()).data().path("meta").path("total").asLong()).isZero();
        var totals = analytics.getEmployeeTips(now.getYear(), now.getMonthValue()).getFirst();
        assertThat(totals.paidTipAmount()).isEqualByComparingTo("2.00");
        assertThat(totals.unpaidTipAmount()).isEqualByComparingTo("0");
        assertThat(request("PUT", "/admin/analytics/tips/payments/" + id + "/unpaid", null,
                adminCookie()).status()).isEqualTo(200);
        assertThat(payments.findById(id).orElseThrow().getTipPaidAt()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "failed", "expired", "cash", "zero-tip", "null-tip"})
    void tipPayoutRejectsIneligiblePayments(String problem) throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long id;
        if (problem.equals("cash")) {
            closeCash(fixture);
            id = payments.findAll().getFirst().getId();
        } else {
            Payment.PaymentStatus status = switch (problem) {
                case "pending" -> Payment.PaymentStatus.PENDING;
                case "failed" -> Payment.PaymentStatus.FAILED;
                case "expired" -> Payment.PaymentStatus.EXPIRED;
                default -> Payment.PaymentStatus.SUCCEEDED;
            };
            id = addOnlinePayment(fixture, status);
            if (problem.equals("zero-tip") || problem.equals("null-tip")) {
                jdbc.update("UPDATE payments SET tip_amount = ? WHERE id = ?",
                        problem.equals("null-tip") ? null : BigDecimal.ZERO, id);
            }
        }
        assertThat(request("PUT", "/admin/analytics/tips/payments/" + id + "/paid", null, adminCookie())
                .status()).isEqualTo(409);
    }

    @Test
    void tipsRemainGroupedByCurrencyAndDeletedEmployeesAndSkipUnassignedRecipients() {
        Fixture uah = fixture(Restaurant.Currency.UAH);
        addOnlinePayment(uah, Payment.PaymentStatus.SUCCEEDED);
        Fixture eur = fixture(Restaurant.Currency.EUR);
        addOnlinePayment(eur, Payment.PaymentStatus.SUCCEEDED);
        Long unassigned = addOnlinePayment(fixture(Restaurant.Currency.UAH), Payment.PaymentStatus.SUCCEEDED);
        jdbc.update("UPDATE payments SET tip_employee_id = NULL WHERE id = ?", unassigned);
        jdbc.update("UPDATE orders SET status = 'CLOSED'");
        employeeService.deleteEmployee(2L);
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        var report = analytics.getEmployeeTips(now.getYear(), now.getMonthValue());
        assertThat(report).hasSize(2).allMatch(row -> row.employeeId().equals(2L));
        assertThat(report).extracting(row -> row.currency())
                .containsExactlyInAnyOrder(Restaurant.Currency.UAH, Restaurant.Currency.EUR);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/monthly-summary?year=1969&month=1", "/monthly-summary?year=2030&month=0",
            "/revenue-summary?year=2030&month=13", "/tips/employees?year=2030&month=0",
            "/tips/payments?year=2030&month=13"})
    void invalidReportRangesReturnBadRequest(String query) throws Exception {
        assertThat(request("GET", "/admin/analytics" + query, null, adminCookie()).status()).isEqualTo(400);
    }
    @Test
    void revenueSeparatesMethodsCurrenciesTipsAndOrderCounts() {
        Fixture cash = fixture(Restaurant.Currency.UAH);
        closeCash(cash);
        Fixture online = fixture(Restaurant.Currency.EUR);
        addOnlinePayment(online, Payment.PaymentStatus.SUCCEEDED);
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        var summary = analytics.getRevenueSummary(now.getYear(), now.getMonthValue());
        assertThat(summary.currencies()).hasSize(2);
        var uah = summary.currencies().stream()
                .filter(row -> row.currency() == Restaurant.Currency.UAH).findFirst().orElseThrow();
        var eur = summary.currencies().stream()
                .filter(row -> row.currency() == Restaurant.Currency.EUR).findFirst().orElseThrow();
        assertThat(uah.cashRevenue()).isEqualByComparingTo("20.00");
        assertThat(uah.onlineRevenue()).isEqualByComparingTo("0");
        assertThat(eur.onlineRevenue()).isEqualByComparingTo("20.00");
        assertThat(eur.onlineTips()).isEqualByComparingTo("2.00");
        assertThat(eur.orderCount()).isEqualTo(1);
        assertThat(analytics.getMonthlySummary(now.getYear(), now.getMonthValue(),
                Restaurant.Currency.UAH).revenueAmount()).isEqualByComparingTo("0");
    }

    @Test
    void orderReassignmentDoesNotMoveExistingTips() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long id = addOnlinePayment(fixture, Payment.PaymentStatus.SUCCEEDED);
        orderService.reassignEmployee(fixture.orderId(), 3L);
        assertThat(payments.findById(id).orElseThrow().getTipEmployee().getId()).isEqualTo(2L);
    }

}
