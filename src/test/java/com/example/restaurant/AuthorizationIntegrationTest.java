package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.restaurant.support.AbstractIntegrationTest;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class AuthorizationIntegrationTest extends AbstractIntegrationTest {
    @ParameterizedTest(name = "anonymous cannot call {0}")
    @MethodSource("protectedRoutes")
    void anonymousCannotCallProtectedBusinessEndpoints(Route route) throws Exception {
        ApiResult response = call(route, null);
        assertThat(response.status()).as(route.toString()).isEqualTo(403);
    }

    @ParameterizedTest(name = "employee cannot call {0}")
    @MethodSource("administratorRoutes")
    void regularEmployeeCannotCallAdministrativeEndpoints(Route route) throws Exception {
        ApiResult response = call(route, employeeCookie());
        assertThat(response.status()).as(route.toString()).isEqualTo(403);
    }

    @ParameterizedTest
    @ValueSource(strings = {"malformed", "wrong-type", "wrong-signature", "expired", "unknown-user"})
    void invalidAccessTokensNeverAuthenticate(String kind) throws Exception {
        String secret = "testing-access-secret-at-least-thirty-two-bytes";
        if (kind.equals("wrong-signature")) {
            secret = "a-different-signing-key-with-at-least-thirty-two-bytes";
        }
        var key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token = io.jsonwebtoken.Jwts.builder()
                .subject(kind.equals("unknown-user") ? "missing@example.com" : employees.findById(2L).orElseThrow().getEmail())
                .claim("tokenType", kind.equals("wrong-type") ? "REFRESH_TOKEN" : "ACCESS_TOKEN")
                .issuedAt(new Date(System.currentTimeMillis() - 60_000))
                .expiration(new Date(System.currentTimeMillis() + (kind.equals("expired") ? -1000 : 60_000)))
                .signWith(key).compact();
        assertThat(request("GET", "/employees/me", null,
                "accessToken=" + (kind.equals("malformed") ? "not-a-jwt" : token)).status()).isEqualTo(403);
    }

    @Test
    void corsAcceptsConfiguredFrontendAndRejectsUntrustedOrigin() throws Exception {
        HttpRequest allowed = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/employees/2"))
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "PUT")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
        var accepted = HTTP.send(allowed, HttpResponse.BodyHandlers.discarding());
        assertThat(accepted.statusCode()).isEqualTo(200);
        assertThat(accepted.headers().firstValue("access-control-allow-origin")).contains("http://localhost:3000");
        HttpRequest denied = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/employees/2"))
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "PUT")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
        assertThat(HTTP.send(denied, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(403);
    }

    private ApiResult call(Route route, String cookie) throws Exception {
        if (route.part() != null) {
            Map<String, Object> parts = route.part().isEmpty() ? Map.of() : Map.of(route.part(), route.body());
            return multipart(route.method(), route.path(), parts, false, cookie);
        }
        return request(route.method(), route.path(), route.body(), cookie);
    }

    static Stream<Route> administratorRoutes() {
        return Stream.of(
                new Route("GET", "/employees", null, null),
                new Route("GET", "/employees/2", null, null),
                new Route("DELETE", "/employees/2", null, null),
                new Route("PUT", "/employees/2", Map.of("firstName", "Updated"), "employeeRequest"),
                new Route("POST", "/auth/register-employee", Map.of("firstName", "Olena", "lastName", "Name",
                        "email", "new@example.com", "phone", "0501112233", "isAdmin", false), "employeeRequest"),
                new Route("POST", "/auth/trigger-finalize-employee-registration", Map.of("email", "new@example.com"), null),
                new Route("POST", "/menu-categories", Map.of("nameDefaultLanguage", "Category"), null),
                new Route("POST", "/menu-categories/parents/1/children", Map.of("nameDefaultLanguage", "Child"), null),
                new Route("PUT", "/menu-categories/parents/1", Map.of("nameDefaultLanguage", "Parent"), null),
                new Route("PUT", "/menu-categories/children/3", Map.of("nameDefaultLanguage", "Child"), null),
                new Route("DELETE", "/menu-categories/parents/1", null, null),
                new Route("DELETE", "/menu-categories/children/3", null, null),
                new Route("POST", "/menu-items", Map.of("nameDefaultLanguage", "Dish", "price", 10,
                        "childCategoryId", 3, "isAllergenic", false, "isVegetarian", false, "isVegan", false), "menuItem"),
                new Route("PUT", "/menu-items/1", Map.of("price", 10), "menuItem"),
                new Route("DELETE", "/menu-items/1", null, null),
                new Route("PUT", "/restaurant/this", null, ""),
                new Route("PUT", "/schedule/1", Map.of("isActive", true), null),
                new Route("GET", "/admin/employee-shifts", null, null),
                new Route("GET", "/admin/employee-shifts/1", null, null),
                new Route("POST", "/admin/employee-shifts", Map.of("employeeId", 2, "workDate", "2030-01-01",
                        "startsAt", "09:00:00", "endsAt", "17:00:00"), null),
                new Route("PUT", "/admin/employee-shifts/1", Map.of("note", "Note"), null),
                new Route("DELETE", "/admin/employee-shifts/1", null, null),
                new Route("POST", "/dining-tables", null, null),
                new Route("PUT", "/dining-tables/1?isActive=false", null, null),
                new Route("PUT", "/orders/1/employee", Map.of("employeeId", 3), null),
                new Route("GET", "/admin/analytics/orders", null, null),
                new Route("GET", "/admin/analytics/monthly-summary?year=2030&month=1", null, null),
                new Route("GET", "/admin/analytics/revenue-summary?year=2030&month=1", null, null),
                new Route("GET", "/admin/analytics/tips/employees?year=2030&month=1", null, null),
                new Route("GET", "/admin/analytics/tips/payments?year=2030&month=1", null, null),
                new Route("PUT", "/admin/analytics/tips/payments/1/paid", null, null),
                new Route("PUT", "/admin/analytics/tips/payments/1/unpaid", null, null),
                new Route("GET", "/reviews", null, null)
        );
    }

    static Stream<Route> protectedRoutes() {
        return Stream.concat(administratorRoutes(), Stream.of(
                new Route("GET", "/employees/me", null, null),
                new Route("PUT", "/employees/me", Map.of("firstName", "Name"), "employeeRequest"),
                new Route("POST", "/auth/logout", null, null),
                new Route("POST", "/auth/change-password", Map.of("currentPassword", PASSWORD,
                        "newPassword", "NewStr0ngPass!2026", "confirmPassword", "NewStr0ngPass!2026"), null),
                new Route("GET", "/dining-tables", null, null),
                new Route("GET", "/dining-tables/1", null, null),
                new Route("GET", "/dining-tables/qr/uuid", null, null),
                new Route("GET", "/orders", null, null),
                new Route("GET", "/orders/1", null, null),
                new Route("GET", "/orders/history", null, null),
                new Route("GET", "/orders/1/payments", null, null),
                new Route("POST", "/orders/1/items", Map.of("menuItemId", 2, "quantity", 1), null),
                new Route("PUT", "/orders/items/1", Map.of("employeeComment", "Comment"), null),
                new Route("PUT", "/orders/1/close", null, null),
                new Route("PUT", "/orders/1/close-manually", Map.of("markUnpaidAs", "CANCELED"), null),
                new Route("GET", "/payments", null, null),
                new Route("GET", "/payments/1", null, null),
                new Route("POST", "/payments/1/sync", null, null),
                new Route("GET", "/reviews/invitations/1", null, null)
        ));
    }

    record Route(String method, String path, Object body, String part) {
        @Override
        public String toString() {
            return method + " " + path;
        }
    }
}
