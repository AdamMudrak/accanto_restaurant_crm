package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.exception.types.conflict.OrderIllegalStateException;
import com.example.restaurant.exception.types.notfound.DiningTableNotFoundException;
import com.example.restaurant.service.order.diningtable.DiningTableAdminService;
import com.example.restaurant.support.AbstractIntegrationTest;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.io.ByteArrayInputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DiningTableIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private DiningTableAdminService diningTables;

    @Test
    void tableLifecycleIncludesActiveFilteringAndQrUrl() throws Exception {
        var created = request("POST", "/dining-tables", null, adminCookie());
        assertThat(created.status()).isEqualTo(200);
        long id = created.id();
        String uuid = tables.findById(id).orElseThrow().getQrUuid();
        assertThat(request("GET", "/dining-tables/" + id, null, employeeCookie()).status()).isEqualTo(200);
        byte[] png = request("GET", "/dining-tables/qr/" + uuid, null, employeeCookie()).bytes();
        var source = new BufferedImageLuminanceSource(
                ImageIO.read(new ByteArrayInputStream(png)));
        var bitmap = new BinaryBitmap(new HybridBinarizer(source));
        assertThat(new MultiFormatReader().decode(bitmap).getText())
                .isEqualTo("http://localhost:3000/dining-tables/guest?uuid=" + uuid);
        long active = diningTables.getAllActiveDiningTables(PageRequest.of(0, 100)).totalElements();
        assertThat(diningTables.getActiveDiningTableById(id).id()).isEqualTo(id);
        assertThat(request("PUT", "/dining-tables/" + id + "?isActive=false", null, adminCookie()).status())
                .isEqualTo(200);
        assertThat(diningTables.getAllActiveDiningTables(PageRequest.of(0, 100)).totalElements())
                .isEqualTo(active - 1);
        assertThatThrownBy(() -> diningTables.getActiveDiningTableById(id))
                .isInstanceOf(DiningTableNotFoundException.class);
        assertThat(request("GET", "/dining-tables/qr/" + uuid, null, employeeCookie()).status()).isEqualTo(404);
        assertThat(request("GET", "/dining-tables/guest?uuid=" + uuid, null, null).status()).isEqualTo(404);
    }

    @Test
    void occupiedTableCannotBeDeactivated() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        long id = orders.findById(fixture.orderId()).orElseThrow().getDiningTable().getId();
        assertThatThrownBy(() -> diningTables.updateDiningTableById(id, false))
                .isInstanceOf(OrderIllegalStateException.class);
    }

    @Test
    void qrEntryCreatesCookiesAndOrderOnlyOnceForReturningGuest() throws Exception {
        var table = diningTables.createDiningTable();
        String uuid = tables.findById(table.id()).orElseThrow().getQrUuid();
        var response = request("GET", "/dining-tables/guest?uuid=" + uuid, null, null);
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.headers().allValues("set-cookie")).hasSize(3)
                .allMatch(cookie -> cookie.contains("HttpOnly"));
        String cookies = response.headers().allValues("set-cookie").stream()
                .map(value -> value.split(";")[0]).collect(Collectors.joining("; "));
        assertThat(request("GET", "/dining-tables/guest?uuid=" + uuid, null, cookies).status()).isEqualTo(200);
        assertThat(orders.count()).isEqualTo(1);
        assertThat(guests.count()).isEqualTo(1);
        assertThat(orders.findAll().getFirst().getCurrency()).isEqualTo(Restaurant.Currency.UAH);
    }

    @Test
    void concurrentFirstVisitorsShareOneOrderAndHaveSeparateGuests() throws Exception {
        var created = diningTables.createDiningTable();
        String uuid = tables.findById(created.id()).orElseThrow().getQrUuid();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Void> entry = () -> {
                start.await();
                tableService.interactWithTableByQrUuid(uuid, new MockHttpServletRequest(),
                        new MockHttpServletResponse());
                return null;
            };
            var first = executor.submit(entry);
            var second = executor.submit(entry);
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        }
        assertThat(orders.count()).isEqualTo(1);
        assertThat(guests.count()).isEqualTo(2);
    }

    @Test
    void invalidAndMissingTableReferencesAreRejected() throws Exception {
        assertThat(request("GET", "/dining-tables/" + MISSING_ID, null, employeeCookie()).status()).isEqualTo(404);
        assertThat(request("GET", "/dining-tables/guest?uuid=unknown", null, null).status()).isEqualTo(404);
        assertThat(request("GET", "/dining-tables/guest", null, null).status()).isEqualTo(400);
        assertThat(request("GET", "/dining-tables?page=0&size=2", null, employeeCookie())
                .data().path("items").size()).isEqualTo(2);
    }
}
