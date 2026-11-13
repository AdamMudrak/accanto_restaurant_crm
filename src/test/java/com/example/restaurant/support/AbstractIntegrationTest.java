package com.example.restaurant.support;

import static org.mockito.Mockito.when;

import com.example.restaurant.RestaurantApplication;
import com.example.restaurant.dto.request.order.CloseOrderManuallyRequest;
import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.order.DiningTable;
import com.example.restaurant.entity.order.Guest;
import com.example.restaurant.entity.order.Order;
import com.example.restaurant.entity.order.OrderItem;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.mapper.menu.MenuItemMapper;
import com.example.restaurant.repository.management.RestaurantRepository;
import com.example.restaurant.repository.menu.MenuItemRepository;
import com.example.restaurant.repository.order.DiningTableRepository;
import com.example.restaurant.repository.order.GuestRepository;
import com.example.restaurant.repository.order.OrderItemRepository;
import com.example.restaurant.repository.order.OrderRepository;
import com.example.restaurant.repository.payment.PaymentRepository;
import com.example.restaurant.repository.token.ConfirmationTokenRepository;
import com.example.restaurant.repository.user.EmployeeRepository;
import com.example.restaurant.service.email.EmailSender;
import com.example.restaurant.service.image.ImageService;
import com.example.restaurant.service.management.AdminAnalyticsService;
import com.example.restaurant.service.management.EmployeeShiftService;
import com.example.restaurant.service.management.RestaurantService;
import com.example.restaurant.service.management.ScheduleService;
import com.example.restaurant.service.menu.MenuCategoryService;
import com.example.restaurant.service.menu.MenuItemService;
import com.example.restaurant.service.order.diningtable.DiningTableGuestService;
import com.example.restaurant.service.order.order.OrderEmployeeService;
import com.example.restaurant.service.order.order.OrderGuestService;
import com.example.restaurant.service.payment.PaymentProviderType;
import com.example.restaurant.service.payment.PaymentService;
import com.example.restaurant.service.payment.PaymentSettlementService;
import com.example.restaurant.service.payment.monobank.MonobankPaymentProvider;
import com.example.restaurant.service.payment.monobank.MonobankPublicKeyService;
import com.example.restaurant.service.payment.stripe.StripePaymentProvider;
import com.example.restaurant.service.review.ReviewService;
import com.example.restaurant.service.security.AuthService;
import com.example.restaurant.service.user.EmployeeService;
import com.example.restaurant.service.utils.TokenGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.Cookie;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = RestaurantApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "payment.sync.initial-delay-ms=86400000", "spring.datasource.hikari.maximum-pool-size=6",
        "logging.level.root=WARN", "logging.file.name=target/integration-app.log",
        "spring.config.import=", "spring.liquibase.contexts=demo",
        "front.base.url=http://localhost:3000", "server.path=http://localhost:8080",
        "allowed.origins=http://localhost:3000", "security.cookie.local=true",
        "security.jwt.access.secret.key=testing-access-secret-at-least-thirty-two-bytes",
        "security.jwt.refresh.secret.key=testing-refresh-secret-at-least-thirty-two-bytes",
        "resend.api.key=test-placeholder", "resend.domain.email=no-reply@example.com",
        "payment.monobank.x-token=test", "payment.monobank.redirect-url=http://localhost/result",
        "payment.monobank.webhook-url=http://localhost/webhook",
        "payment.stripe.secret-key=test", "payment.stripe.webhook-secret=test",
        "payment.stripe.success-url=http://localhost/success",
        "payment.stripe.cancel-url=http://localhost/cancel"
})
@Import(AbstractIntegrationTest.SharedTestConfig.class)
public abstract class AbstractIntegrationTest {
    public static final String PASSWORD = "Str0ngPass!2026";
    protected static final long MISSING_ID = 9_000_000_000L;
    protected static final HttpClient HTTP = HttpClient.newHttpClient();

    @Autowired
    protected TransactionTemplate transactions;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected RestaurantRepository restaurants;
    @Autowired
    protected DiningTableRepository tables;
    @Autowired
    protected OrderRepository orders;
    @Autowired
    protected GuestRepository guests;
    @Autowired
    protected OrderItemRepository items;
    @Autowired
    protected MenuItemRepository menu;
    @Autowired
    protected PaymentRepository payments;
    @Autowired
    protected EmployeeRepository employees;
    @Autowired
    protected OrderEmployeeService orderService;
    @Autowired
    protected DiningTableGuestService tableService;
    @Autowired
    protected PaymentService paymentService;
    @Autowired
    protected PaymentSettlementService settlement;
    @Autowired
    protected EmployeeService employeeService;
    @Autowired
    protected RestaurantService restaurantService;
    @Autowired
    protected ReviewService reviews;
    @Autowired
    protected AdminAnalyticsService analytics;
    @Autowired
    protected MenuItemMapper menuMapper;
    @Autowired
    protected TokenGenerator tokens;
    @MockitoBean
    protected MonobankPaymentProvider monobank;
    @MockitoBean
    protected StripePaymentProvider stripe;
    @LocalServerPort
    protected int port;

    protected final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    @Autowired
    protected DatabaseState databaseState;
    @Autowired
    protected OrderGuestService guestOrders;
    @Autowired
    protected MenuCategoryService categories;
    @Autowired
    protected MenuItemService menuService;
    @Autowired
    protected ScheduleService schedule;
    @Autowired
    protected EmployeeShiftService shifts;
    @Autowired
    protected ImageService images;
    @Autowired
    protected AuthService auth;
    @Autowired
    protected ConfirmationTokenRepository confirmationTokens;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @MockitoBean
    protected EmailSender emailSender;
    @MockitoBean
    protected MonobankPublicKeyService monoKeys;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedMySql::url);
        registry.add("spring.datasource.username", SharedMySql::username);
        registry.add("spring.datasource.password", SharedMySql::password);
    }

    @BeforeEach
    void restoreDataAndExternalBoundaries() throws Exception {
        databaseState.restore();
        when(monobank.getType()).thenReturn(PaymentProviderType.MONOBANK);
        when(stripe.getType()).thenReturn(PaymentProviderType.STRIPE);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SharedTestConfig {
        @Bean
        @DependsOn("liquibase")
        DatabaseState databaseState(DataSource dataSource, PasswordEncoder encoder) throws Exception {
            return new DatabaseState(dataSource, encoder);
        }
    }
    protected void closeCash(Fixture fixture) {
        orderService.closeOrderManually(fixture.orderId(), new CloseOrderManuallyRequest(
                CloseOrderManuallyRequest.ManualCloseStatus.PAID_CASH, "Cash received"),
                employees.findById(2L).orElseThrow());
    }

    protected Fixture fixture(Restaurant.Currency currency) {
        return transactions.execute(ignored -> {
            DiningTable table = new DiningTable();
            table.setQrUuid(UUID.randomUUID().toString());
            table.setActive(true);
            tables.save(table);
            Order order = new Order();
            order.setDiningTable(table);
            order.setCurrency(currency);
            order.setUuid(UUID.randomUUID().toString());
            order.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            order.setEmployee(employees.findById(2L).orElseThrow());
            orders.save(order);
            Guest guest = new Guest();
            guest.setOrder(order);
            guest.setUuid(UUID.randomUUID().toString());
            guests.save(guest);
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setGuest(guest);
            item.setMenuItem(menu.findById(1L).orElseThrow());
            item.setItemNameSnapshot("Test item");
            item.setItemPriceSnapshot(new BigDecimal("10.00"));
            item.setQuantity(2);
            item.setStatus(OrderItem.OrderItemStatus.CONFIRMED);
            items.save(item);
            return new Fixture(order.getId(), item.getId(), guest.getId(), guest.getUuid(),
                    order.getUuid(), table.getQrUuid());
        });
    }

    protected Long addOnlinePayment(Fixture fixture, Payment.PaymentStatus status) {
        return transactions.execute(ignored -> {
            OrderItem item = items.findById(fixture.itemId()).orElseThrow();
            Payment payment = new Payment();
            payment.setOrder(item.getOrder());
            payment.setCurrency(item.getOrder().getCurrency());
            payment.setPayerGuest(item.getGuest());
            payment.setTipEmployee(item.getOrder().getEmployee());
            payment.setProviderType(item.getOrder().getCurrency() == Restaurant.Currency.EUR
                    ? PaymentProviderType.STRIPE : PaymentProviderType.MONOBANK);
            payment.setProviderPaymentId(UUID.randomUUID().toString());
            payment.setAmount(new BigDecimal("20.00"));
            payment.setTipAmount(new BigDecimal("2.00"));
            payment.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            payment.setStatus(status);
            if (status == Payment.PaymentStatus.SUCCEEDED) {
                payment.setCompletedAt(payment.getCreatedAt());
                item.setStatus(OrderItem.OrderItemStatus.PAID_CARD);
            }
            payment.getOrderItems().add(item);
            payments.save(payment);
            item.setPayment(payment);
            return payment.getId();
        });
    }

    protected MockHttpServletRequest guestRequest(Fixture fixture) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Idempotency-Key", UUID.randomUUID().toString());
        request.setCookies(new Cookie("guestUuid", fixture.guestUuid()),
                new Cookie("orderUuid", fixture.orderUuid()));
        return request;
    }

    protected int http(String method, String path, String body, String cookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:"
                + port + path)).header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString());
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        return HTTP.send(request.build(),
                HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    protected record Fixture(Long orderId, Long itemId, Long guestId, String guestUuid,
                           String orderUuid, String tableUuid) {}

    protected String adminCookie() {
        return "accessToken=" + tokens.generateAccessToken(employees.findById(1L)
                .orElseThrow().getEmail());
    }

    protected String employeeCookie() {
        return "accessToken=" + tokens.generateAccessToken(employees.findById(2L)
                .orElseThrow().getEmail());
    }

    protected String guestCookie(Fixture fixture) {
        return "guestUuid=" + fixture.guestUuid() + "; orderUuid=" + fixture.orderUuid();
    }

    protected ApiResult request(String method, String path, Object body, String cookie)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:"
                + port + path)).header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString());
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        String payload = body instanceof String text ? text : json.writeValueAsString(body);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(payload));
        return result(HTTP.send(request.build(), HttpResponse.BodyHandlers.ofByteArray()));
    }

    protected ApiResult multipart(String method, String path, Map<String, Object> parts,
                                  boolean withImage, String cookie) throws Exception {
        String boundary = "integration-boundary";
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        for (var part : parts.entrySet()) {
            data.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\""
                    + part.getKey() + "\"\r\nContent-Type: application/json\r\n\r\n"
                    + json.writeValueAsString(part.getValue()) + "\r\n")
                    .getBytes(StandardCharsets.UTF_8));
        }
        if (withImage) {
            data.write(("--" + boundary
                    + "\r\nContent-Disposition: form-data; name=\"itemImage\"; filename=\"test.png\""
                    + "\r\nContent-Type: image/png\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            data.write(png().getBytes());
            data.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        data.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:"
                + port + path)).header("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        builder.method(method, HttpRequest.BodyPublishers.ofByteArray(data.toByteArray()));
        return result(HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray()));
    }

    private ApiResult result(HttpResponse<byte[]> response) {
        JsonNode node;
        try {
            node = json.readTree(response.body());
        } catch (Exception exception) {
            node = NullNode.getInstance();
        }
        return new ApiResult(response.statusCode(), node == null ? NullNode.getInstance() : node,
                response.body(), response.headers());
    }

    protected MockMultipartFile png() {
        byte[] bytes = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l9sAAAAASUVORK5CYII=");
        return new MockMultipartFile("image", "test.png", "image/png",
                bytes);
    }

    protected void setItemStatus(Fixture fixture, OrderItem.OrderItemStatus status) {
        transactions.executeWithoutResult(ignored -> items.findById(fixture.itemId())
                .orElseThrow().setStatus(status));
    }

    protected Fixture additionalItem(Fixture fixture, OrderItem.OrderItemStatus status) {
        return transactions.execute(ignored -> {
            OrderItem item = new OrderItem();
            item.setOrder(orders.findById(fixture.orderId()).orElseThrow());
            item.setGuest(guests.findById(fixture.guestId()).orElseThrow());
            item.setMenuItem(menu.findById(2L).orElseThrow());
            item.setItemNameSnapshot("Additional item");
            item.setItemPriceSnapshot(new BigDecimal("10.00"));
            item.setQuantity(2);
            item.setStatus(status);
            items.save(item);
            return new Fixture(fixture.orderId(), item.getId(), fixture.guestId(),
                    fixture.guestUuid(), fixture.orderUuid(), fixture.tableUuid());
        });
    }

    protected record ApiResult(int status, JsonNode body, byte[] bytes, HttpHeaders headers) {
        public ApiResult {}

        public JsonNode data() {
            return body.path("data");
        }

        public long id() {
            return data().path("id").asLong();
        }
    }
}
