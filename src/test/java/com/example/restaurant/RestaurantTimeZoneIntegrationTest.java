package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;

class RestaurantTimeZoneIntegrationTest extends AbstractIntegrationTest {
    @Test
    void defaultsToKyivAndOnlyAdministratorCanSelectValidIanaZone() throws Exception {
        assertThat(restaurantService.getRestaurant().timeZone()).isEqualTo("Europe/Kyiv");
        assertThat(request("PATCH", "/restaurant/this/time-zone", Map.of("timeZone", "America/New_York"), null).status()).isEqualTo(403);
        assertThat(request("PATCH", "/restaurant/this/time-zone", Map.of("timeZone", "America/New_York"), employeeCookie()).status()).isEqualTo(403);
        var response = request("PATCH", "/restaurant/this/time-zone", Map.of("timeZone", "America/New_York"), adminCookie());
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.data().path("timeZone").asText()).isEqualTo("America/New_York");
        assertThat(request("GET", "/restaurant/this", null, null).data().path("timeZone").asText()).isEqualTo("America/New_York");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Mars/Kyiv", "+03:00", "GMT+3", "Europe/Kiev\ninvalid"})
    void rejectsInvalidTimeZonesWithoutChangingProfile(String zone) throws Exception {
        assertThat(request("PATCH", "/restaurant/this/time-zone", Map.of("timeZone", zone), adminCookie()).status()).isEqualTo(400);
        assertThat(restaurantService.getRestaurant().timeZone()).isEqualTo("Europe/Kyiv");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Europe/Kyiv", "America/New_York", "Asia/Tokyo", "UTC"})
    void allFinancialReportsUseLocalMonthBoundariesAcrossDst(String zone) {
        restaurantService.updateTimeZone(zone);
        ZoneId business = ZoneId.of(zone);
        LocalDateTime from = YearMonth.of(2030, 3).atDay(1).atStartOfDay(business)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime until = YearMonth.of(2030, 4).atDay(1).atStartOfDay(business)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        for (LocalDateTime moment : new LocalDateTime[]{from.minusNanos(1000), from, until.minusNanos(1000), until}) {
            long payment = addOnlinePayment(fixture(Restaurant.Currency.UAH), Payment.PaymentStatus.SUCCEEDED);
            jdbc.update("UPDATE payments SET completed_at = ? WHERE id = ?", moment, payment);
        }
        assertThat(analytics.getRevenueSummary(2030, 3).timeZone()).isEqualTo(zone);
        assertThat(analytics.getRevenueSummary(2030, 3).currencies().getFirst().paymentCount()).isEqualTo(2);
        assertThat(analytics.getMonthlySummary(2030, 3, Restaurant.Currency.UAH).succeededPaymentsCount()).isEqualTo(2);
        assertThat(analytics.getEmployeeTips(2030, 3).getFirst().paymentCount()).isEqualTo(2);
        assertThat(analytics.getTipPayments(2030, 3, null, PageRequest.of(0, 10)).totalElements()).isEqualTo(2);
    }
}
