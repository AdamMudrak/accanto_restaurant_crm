package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class GuestSessionIntegrationTest extends AbstractIntegrationTest {
    @Test
    void staleGuestCookieIsReplacedAndCurrentGuestIsReused() {
        Fixture previous = fixture(Restaurant.Currency.UAH);
        Fixture current = fixture(Restaurant.Currency.UAH);
        MockHttpServletResponse response = new MockHttpServletResponse();
        tableService.interactWithTableByQrUuid(current.tableUuid(), guestRequest(previous), response);
        Cookie replacement = response.getCookie("guestUuid");
        assertThat(replacement).isNotNull();
        assertThat(replacement.getValue()).isNotEqualTo(previous.guestUuid());
        assertThat(guests.findByUuid(previous.guestUuid())).isPresent();
        long guestCount = guests.count();
        MockHttpServletRequest request = guestRequest(current);
        request.setCookies(replacement);
        tableService.interactWithTableByQrUuid(current.tableUuid(), request,
                new MockHttpServletResponse());
        assertThat(guests.count()).isEqualTo(guestCount);
    }

    @Test
    void repeatVisitAtSameTableCreatesNewOrderAndPreservesPreviousGuest() {
        Fixture previous = fixture(Restaurant.Currency.UAH);
        closeCash(previous);
        MockHttpServletResponse response = new MockHttpServletResponse();
        tableService.interactWithTableByQrUuid(previous.tableUuid(), guestRequest(previous), response);
        assertThat(response.getCookie("orderUuid").getValue()).isNotEqualTo(previous.orderUuid());
        assertThat(response.getCookie("guestUuid").getValue()).isNotEqualTo(previous.guestUuid());
        assertThat(guests.findByUuid(previous.guestUuid())).isPresent();
        assertThat(orders.count()).isEqualTo(2);
    }

    @Test
    void deletedGuestCookieCreatesNewGuest() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("guestUuid", UUID.randomUUID().toString()));
        MockHttpServletResponse response = new MockHttpServletResponse();
        tableService.interactWithTableByQrUuid(fixture.tableUuid(), request, response);
        assertThat(response.getCookie("guestUuid")).isNotNull();
        assertThat(guests.count()).isEqualTo(2);
    }

}
