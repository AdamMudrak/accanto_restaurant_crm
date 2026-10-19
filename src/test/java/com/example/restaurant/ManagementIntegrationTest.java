package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.restaurant.dto.request.management.CreateEmployeeShiftRequest;
import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.exception.types.conflict.OrderIllegalStateException;
import com.example.restaurant.exception.types.conflict.TimeConflictException;
import com.example.restaurant.support.AbstractIntegrationTest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;

class ManagementIntegrationTest extends AbstractIntegrationTest {
    @Test
    void publicScheduleIsOrderedAndAdministratorCanSetHoursAndActivity() throws Exception {
        var list = request("GET", "/schedule", null, null);
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.data().path("items").size()).isEqualTo(7);
        assertThat(list.data().path("items").get(0).path("id").asLong()).isEqualTo(1);
        var changed = request("PUT", "/schedule/1", Map.of("isActive", true,
                "openAt", "10:00:00", "closeAt", "20:00:00"), adminCookie());
        assertThat(changed.status()).isEqualTo(200);
        assertThat(request("GET", "/schedule/1", null, null).data().path("openAt").asText())
                .isEqualTo("10:00:00");
        assertThat(request("PUT", "/schedule/1", Map.of("openAt", "11:00:00"),
                adminCookie()).status()).isEqualTo(200);
        assertThat(request("PUT", "/schedule/1", Map.of("closeAt", "19:00:00"),
                adminCookie()).status()).isEqualTo(200);
        assertThat(request("PUT", "/schedule/1", Map.of(), adminCookie()).status()).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"openAt\":\"20:00:00\"}", "{\"closeAt\":\"08:00:00\"}",
            "{\"openAt\":\"18:00:00\"}", "{\"closeAt\":\"09:00:00\"}"})
    void partialScheduleUpdatesCannotInvertOpeningHours(String body) throws Exception {
        assertThat(request("PUT", "/schedule/1", body, adminCookie()).status()).isEqualTo(409);
        assertThat(schedule.getWorkingDayById(1L).openAt()).isEqualTo(LocalTime.of(9, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"openAt\":\"20:00:00\",\"closeAt\":\"10:00:00\"}",
            "{\"openAt\":\"09:00:00\",\"closeAt\":\"09:00:00\"}"})
    void invalidFullScheduleTimeRangeIsValidated(String body) throws Exception {
        assertThat(request("PUT", "/schedule/1", body, adminCookie()).status()).isEqualTo(400);
    }

    @Test
    void missingWorkingDayAndShiftReturnNotFound() throws Exception {
        assertThat(request("GET", "/schedule/" + MISSING_ID, null, null).status()).isEqualTo(404);
        assertThat(request("PUT", "/schedule/" + MISSING_ID, Map.of(), adminCookie()).status())
                .isEqualTo(404);
        assertThat(request("GET", "/admin/employee-shifts/" + MISSING_ID, null, adminCookie()).status())
                .isEqualTo(404);
        assertThat(request("DELETE", "/admin/employee-shifts/" + MISSING_ID, null, adminCookie()).status())
                .isEqualTo(404);
    }

    @Test
    void employeeShiftLifecycleAndDateEmployeeFiltersWorkThroughApi() throws Exception {
        var created = request("POST", "/admin/employee-shifts", Map.of(
                "employeeId", 2, "workDate", "2030-01-10", "startsAt", "09:00:00",
                "endsAt", "17:00:00", "note", "Morning"), adminCookie());
        assertThat(created.status()).isEqualTo(201);
        long id = created.id();
        assertThat(request("GET", "/admin/employee-shifts/" + id, null, adminCookie()).status()).isEqualTo(200);
        var updated = request("PUT", "/admin/employee-shifts/" + id, Map.of(
                "employeeId", 3, "workDate", "2030-01-11", "startsAt", "10:00:00",
                "endsAt", "18:00:00", "note", "Later"), adminCookie());
        assertThat(updated.status()).isEqualTo(200);
        assertThat(updated.data().path("employeeId").asLong()).isEqualTo(3);
        assertThat(request("PUT", "/admin/employee-shifts/" + id, Map.of(), adminCookie())
                .status()).isEqualTo(200);
        assertThat(request("GET", "/admin/employee-shifts?employeeId=3&from=2030-01-01&to=2030-01-31",
                null, adminCookie()).data().path("meta").path("total").asLong()).isEqualTo(1);
        assertThat(request("GET", "/admin/employee-shifts?employeeId=2", null, adminCookie())
                .data().path("meta").path("total").asLong()).isZero();
        assertThat(request("DELETE", "/admin/employee-shifts/" + id, null, adminCookie()).status())
                .isEqualTo(200);
    }

    @ParameterizedTest
    @CsvSource({"09:00,17:00", "08:00,10:00", "16:00,20:00", "08:00,20:00"})
    void overlappingShiftsAreRejected(LocalTime start, LocalTime end) {
        var date = LocalDate.of(2030, 1, 10);
        shifts.createEmployeeShift(new CreateEmployeeShiftRequest(
                2L, date, LocalTime.of(9, 0), LocalTime.of(17, 0), null));
        assertThatThrownBy(() -> shifts.createEmployeeShift(
                new CreateEmployeeShiftRequest(2L, date,
                        start, end, null)))
                .isInstanceOf(TimeConflictException.class);
    }

    @Test
    void adjacentShiftsDifferentEmployeesAndDifferentDaysDoNotConflict() {
        var date = LocalDate.of(2030, 1, 10);
        for (long employeeId : List.of(2L, 3L)) {
            shifts.createEmployeeShift(new CreateEmployeeShiftRequest(
                    employeeId, date, LocalTime.of(9, 0), LocalTime.of(17, 0), null));
        }
        shifts.createEmployeeShift(new CreateEmployeeShiftRequest(
                2L, date, LocalTime.of(17, 0), LocalTime.of(19, 0), null));
        shifts.createEmployeeShift(new CreateEmployeeShiftRequest(
                2L, date.plusDays(1), LocalTime.of(9, 0), LocalTime.of(17, 0), null));
        assertThat(shifts.getEmployeeShifts(null, null, null, PageRequest.of(0, 10)).totalElements()).isEqualTo(4);
    }

    @Test
    void shiftUpdateCannotOverlapAnotherShiftButCanKeepOwnHours() throws Exception {
        var first = request("POST", "/admin/employee-shifts", Map.of("employeeId", 2,
                "workDate", "2030-01-10", "startsAt", "09:00:00", "endsAt", "12:00:00"), adminCookie());
        var second = request("POST", "/admin/employee-shifts", Map.of("employeeId", 2,
                "workDate", "2030-01-10", "startsAt", "13:00:00", "endsAt", "17:00:00"), adminCookie());
        assertThat(request("PUT", "/admin/employee-shifts/" + first.id(), Map.of(), adminCookie())
                .status()).isEqualTo(200);
        assertThat(request("PUT", "/admin/employee-shifts/" + second.id(),
                Map.of("startsAt", "11:00:00"), adminCookie()).status()).isEqualTo(409);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "deleted", "invalid-range", "equal-range", "missing-fields"})
    void invalidShiftRequestsDoNotCreateShifts(String problem) throws Exception {
        if (problem.equals("deleted")) {
            employeeService.deleteEmployee(3L);
        }
        Map<String, Object> body = new HashMap<>();
        if (!problem.equals("missing-fields")) {
            body.put("employeeId", problem.equals("missing") ? MISSING_ID : 3L);
            body.put("workDate", "2030-01-10");
            body.put("startsAt", "17:00:00");
            body.put("endsAt", problem.equals("equal-range") ? "17:00:00" : "09:00:00");
        }
        int expected = problem.equals("missing") || problem.equals("deleted") ? 404
                : problem.equals("missing-fields") ? 400 : 409;
        assertThat(request("POST", "/admin/employee-shifts", body, adminCookie()).status()).isEqualTo(expected);
        assertThat(shifts.getEmployeeShifts(null, null, null, PageRequest.of(0, 10)).totalElements()).isZero();
    }

    @Test
    void restaurantProfileAndImagesCanBeChangedAndRegionSettingsStayConsistent() throws Exception {
        var before = restaurantService.getRestaurant();
        var updated = restaurantService.updateRestaurant("New Restaurant", null, false, png(), png());
        assertThat(updated.name()).isEqualTo("New Restaurant");
        assertThat(updated.logoId()).isNotEqualTo(before.logoId());
        assertThat(updated.backgroundImageId()).isNotEqualTo(before.backgroundImageId());
        restaurantService.updateRestaurant(null, Restaurant.DefaultRegion.EUROPE, true, null, null);
        assertThat(restaurantService.getRestaurant().currency()).isEqualTo(Restaurant.Currency.EUR);
        assertThat(restaurantService.getRestaurant().language()).isEqualTo(Restaurant.Language.EN);
        restaurantService.updateRestaurant(null, Restaurant.DefaultRegion.UKRAINE, true, null, null);
        assertThat(request("GET", "/restaurant/this", null, null).data().path("currency").asText())
                .isEqualTo("UAH");
        assertThat(multipart("PUT", "/restaurant/this?name=HTTP-Name", Map.of(), false,
                adminCookie()).status()).isEqualTo(200);
    }
    @Test
    void currencyChangeRequiresClosedOrdersAndExplicitPriceConfirmation() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        assertThatThrownBy(() -> restaurantService.updateRestaurant(null,
                Restaurant.DefaultRegion.EUROPE, true, null, null))
                .isInstanceOf(OrderIllegalStateException.class);
        closeCash(fixture);
        assertThatThrownBy(() -> restaurantService.updateRestaurant(null,
                Restaurant.DefaultRegion.EUROPE, false, null, null))
                .isInstanceOf(OrderIllegalStateException.class);
        restaurantService.updateRestaurant(null, Restaurant.DefaultRegion.EUROPE, true, null, null);
        assertThat(orders.findById(fixture.orderId()).orElseThrow().getCurrency())
                .isEqualTo(Restaurant.Currency.UAH);
        assertThat(payments.findAll().getFirst().getCurrency()).isEqualTo(Restaurant.Currency.UAH);
        Restaurant.Currency menuCurrency = transactions.execute(ignored ->
                menuMapper.toMenuItemResponse(menu.findById(1L).orElseThrow()).currency());
        assertThat(menuCurrency).isEqualTo(Restaurant.Currency.EUR);
    }

}
