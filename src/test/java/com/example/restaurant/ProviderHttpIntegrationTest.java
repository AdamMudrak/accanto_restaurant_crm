package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.restaurant.exception.types.internalserver.MonobankPaymentProviderException;
import com.example.restaurant.exception.types.internalserver.StripePaymentProviderException;
import com.example.restaurant.repository.payment.MonobankPublicKeyRepository;
import com.example.restaurant.service.payment.monobank.MonobankPaymentProvider;
import com.example.restaurant.service.payment.monobank.MonobankPublicKeyService;
import com.example.restaurant.service.payment.monobank.dto.request.MonobankInvoiceRequest;
import com.example.restaurant.service.payment.monobank.dto.request.MonobankStatusRequest;
import com.example.restaurant.service.payment.stripe.StripePaymentProvider;
import com.example.restaurant.service.payment.stripe.dto.request.StripeCheckoutSessionRequest;
import com.example.restaurant.service.payment.stripe.dto.request.StripeStatusRequest;
import com.example.restaurant.service.utils.JsonConverter;
import com.example.restaurant.support.AbstractIntegrationTest;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/** Real provider HTTP clients against a loopback server; never calls a real payment provider. */
class ProviderHttpIntegrationTest extends AbstractIntegrationTest {
    private static HttpServer provider;
    private static ExecutorService workers;
    private static String baseUrl;
    private static final AtomicReference<Reply> RESPONSE = new AtomicReference<>();
    private static final Map<String, Reply> ROUTES = new java.util.concurrent.ConcurrentHashMap<>();
    private static final List<RecordedRequest> REQUESTS = new CopyOnWriteArrayList<>();
    @Autowired
    private MonobankPublicKeyRepository publicKeys;
    private MonobankPaymentProvider monoClient;
    private StripePaymentProvider stripeClient;
    private MonobankPublicKeyService keyClient;

    @BeforeAll
    static void startLoopbackProvider() throws Exception {
        provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        workers = Executors.newFixedThreadPool(2);
        provider.setExecutor(workers);
        provider.createContext("/", exchange -> {
            REQUESTS.add(new RecordedRequest(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                    exchange.getRequestHeaders(), new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            Reply reply = ROUTES.getOrDefault(exchange.getRequestURI().getPath(), RESPONSE.get());
            try {
                if (reply.delayMillis() > 0) {
                    Thread.sleep(reply.delayMillis());
                }
                byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(reply.status(), bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        provider.start();
        baseUrl = "http://127.0.0.1:" + provider.getAddress().getPort();
    }

    @AfterAll
    static void stopLoopbackProvider() {
        provider.stop(0);
        workers.shutdownNow();
    }

    @BeforeEach
    void realClientsAndDefaultResponse() {
        RESPONSE.set(new Reply(200, "{}", 0));
        REQUESTS.clear();
        ROUTES.clear();
        JsonConverter converter = new JsonConverter();
        monoClient = new MonobankPaymentProvider(converter);
        stripeClient = new StripePaymentProvider(converter);
        keyClient = new MonobankPublicKeyService(publicKeys, converter);
        for (Object client : List.of(monoClient, stripeClient, keyClient)) {
            ReflectionTestUtils.setField(client, "baseUrl", baseUrl);
        }
        ReflectionTestUtils.setField(monoClient, "monoXToken", "mono-test-token");
        ReflectionTestUtils.setField(keyClient, "monoXToken", "mono-test-token");
        ReflectionTestUtils.setField(stripeClient, "secretKey", "stripe-test-secret");
    }

    @Test
    void stripeRecoveryVerifiesInvoiceAndFullRefundUsesPaymentIntentAndStableIdempotencyKey() {
        ROUTES.put("/v1/checkout/sessions/cs_paid", new Reply(200,
                "{\"id\":\"cs_paid\",\"client_reference_id\":\"42\",\"amount_total\":2200,\"currency\":\"eur\",\"payment_status\":\"paid\",\"payment_intent\":\"pi_paid\"}", 0));
        ROUTES.put("/v1/refunds", new Reply(200,
                "{\"id\":\"re_full\",\"amount\":2200,\"currency\":\"eur\",\"status\":\"pending\",\"payment_intent\":\"pi_paid\"}", 0));
        var invoice = stripeClient.inspect("cs_paid");
        assertThat(invoice.reference()).isEqualTo("42");
        assertThat(invoice.amount()).isEqualTo(2200);
        assertThat(invoice.status()).isEqualTo(com.example.restaurant.entity.payment.Payment.PaymentStatus.SUCCEEDED);
        var refund = stripeClient.refund("cs_paid", "refund-42-1", 2200, "EUR");
        assertThat(refund.status()).isEqualTo(com.example.restaurant.entity.payment.PaymentRefund.Status.PENDING);
        var sent = REQUESTS.getLast();
        assertThat(sent.path()).isEqualTo("/v1/refunds");
        assertThat(sent.headers().get("Idempotency-key")).containsExactly("refund-42-1");
        assertThat(sent.body()).contains("payment_intent=pi_paid", "amount=2200", "reference");
        ROUTES.put("/v1/refunds/re_full", new Reply(200,
                "{\"id\":\"re_full\",\"amount\":2200,\"currency\":\"eur\",\"status\":\"succeeded\",\"payment_intent\":\"pi_paid\"}", 0));
        assertThat(stripeClient.inspectRefund("cs_paid", "refund-42-1", "re_full", 2200, "EUR").status())
                .isEqualTo(com.example.restaurant.entity.payment.PaymentRefund.Status.SUCCEEDED);
        ROUTES.put("/v1/refunds/re_foreign", new Reply(200,
                "{\"id\":\"re_foreign\",\"amount\":2200,\"currency\":\"eur\",\"status\":\"succeeded\",\"payment_intent\":\"pi_other\"}", 0));
        assertThatThrownBy(() -> stripeClient.inspectRefund("cs_paid", "r", "re_foreign", 2200, "EUR"))
                .hasMessageContaining("another checkout");
        assertThatThrownBy(() -> stripeClient.inspectRefund("cs_paid", "r", null, 2200, "EUR"))
                .hasMessageContaining("Unknown refund result");
    }

    @ParameterizedTest
    @ValueSource(strings = {"processing", "success", "failure"})
    void monobankRecoveryAndRefundContractVerifiesCancellationReferenceAmountAndCurrency(String state) {
        String body = "{\"invoiceId\":\"mono_paid\",\"reference\":\"42\",\"amount\":2200,\"ccy\":980,\"status\":\"success\",\"cancelList\":[{\"extRef\":\"refund-42-1\",\"amount\":2200,\"ccy\":980,\"status\":\"" + state + "\"}]}";
        ROUTES.put("/api/merchant/invoice/status", new Reply(200, body, 0));
        ROUTES.put("/api/merchant/invoice/cancel", new Reply(200, "{}", 0));
        var invoice = monoClient.inspect("mono_paid");
        assertThat(invoice.reference()).isEqualTo("42");
        assertThat(invoice.currency()).isEqualTo("UAH");
        var refund = monoClient.refund("mono_paid", "refund-42-1", 2200, "UAH");
        var expected = switch (state) {
            case "success" -> com.example.restaurant.entity.payment.PaymentRefund.Status.SUCCEEDED;
            case "failure" -> com.example.restaurant.entity.payment.PaymentRefund.Status.FAILED;
            default -> com.example.restaurant.entity.payment.PaymentRefund.Status.PENDING;
        };
        assertThat(refund.status()).isEqualTo(expected);
        assertThat(refund.amount()).isEqualTo(2200);
        var sent = REQUESTS.stream().filter(r -> r.method().equals("POST")).findFirst().orElseThrow();
        assertThat(sent.path()).isEqualTo("/api/merchant/invoice/cancel");
        assertThat(sent.headers().get("X-token")).containsExactly("mono-test-token");
        assertThat(sent.body()).contains("refund-42-1", "2200", "mono_paid");
    }

    @Test
    void monobankInvoiceHttpContractIncludesHeadersPayloadAndProviderResponse() throws Exception {
        RESPONSE.set(new Reply(200, "{\"invoiceId\":\"invoice\",\"pageUrl\":\"https://pay.example\"}", 0));
        var response = monoClient.pay(invoice());
        assertThat(response.invoiceId()).isEqualTo("invoice");
        assertThat(response.pageUrl()).isEqualTo("https://pay.example");
        RecordedRequest sent = REQUESTS.getFirst();
        assertThat(sent.method()).isEqualTo("POST");
        assertThat(sent.path()).isEqualTo("/api/merchant/invoice/create");
        assertThat(sent.headers().get("X-token")).containsExactly("mono-test-token");
        var payload = json.readTree(sent.body());
        assertThat(payload.path("amount").asLong()).isEqualTo(2250);
        assertThat(payload.path("ccy").asInt()).isEqualTo(980);
        assertThat(payload.path("merchantPaymInfo").path("reference").asText()).isEqualTo("123");
        assertThat(payload.has("displayType")).isFalse();
    }

    @Test
    void stripeCheckoutContractEncodesFormAndProvidesStableIdempotencyKey() throws Exception {
        RESPONSE.set(new Reply(201, "{\"id\":\"cs_test\",\"url\":\"https://pay.example\",\"status\":\"open\",\"payment_status\":\"unpaid\"}", 0));
        var response = stripeClient.pay(checkout());
        assertThat(response.id()).isEqualTo("cs_test");
        var sent = REQUESTS.getFirst();
        assertThat(sent.headers().get("Authorization")).containsExactly("Bearer stripe-test-secret");
        assertThat(sent.headers().get("Idempotency-key")).containsExactly("restaurant-payment-123");
        var form = Arrays.stream(sent.body().split("&")).map(part -> part.split("=", 2))
                .collect(Collectors.toMap(pair -> java.net.URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
        assertThat(form).containsEntry("line_items[0][price_data][unit_amount]", "2250")
                .containsEntry("line_items[0][price_data][currency]", "eur")
                .containsEntry("line_items[0][price_data][product_data][name]", "Order + tips & dinner")
                .containsEntry("metadata[paymentId]", "123");
    }

    @Test
    void providerStatusHttpRequestsEncodeInvoiceIdsAndParseStates() {
        RESPONSE.set(new Reply(200, "{\"invoiceId\":\"invoice\",\"status\":\"success\"}", 0));
        assertThat(monoClient.getStatus(new MonobankStatusRequest("id & test")).status().name()).isEqualTo("success");
        assertThat(REQUESTS.getFirst().path()).isEqualTo("/api/merchant/invoice/status?invoiceId=id+%26+test");
        RESPONSE.set(new Reply(200, "{\"id\":\"cs_test\",\"payment_status\":\"paid\",\"status\":\"complete\"}", 0));
        assertThat(stripeClient.getStatus(new StripeStatusRequest("id/test")).paymentStatus()).isEqualTo("paid");
        assertThat(REQUESTS.getLast().path()).isEqualTo("/v1/checkout/sessions/id%2Ftest");
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 429, 500, 503})
    void nonSuccessProviderResponsesAreNotTreatedAsSuccessfulPayments(int status) {
        RESPONSE.set(new Reply(status, "{\"error\":\"unavailable\"}", 0));
        assertThatThrownBy(() -> monoClient.pay(invoice())).hasMessageContaining(Integer.toString(status));
        assertThatThrownBy(() -> stripeClient.pay(checkout())).hasMessageContaining(Integer.toString(status));
        assertThatThrownBy(() -> keyClient.refreshPublicKey()).hasMessageContaining(Integer.toString(status));
        assertThat(publicKeys.count()).isZero();
    }

    @Test
    void malformedProviderJsonIsRejectedByBothPaymentAdapters() {
        RESPONSE.set(new Reply(200, "{", 0));
        assertThatThrownBy(() -> monoClient.pay(invoice())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stripeClient.pay(checkout())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publicKeyIsCachedInDatabaseAndRefreshReplacesSameRecord() {
        RESPONSE.set(new Reply(200, "{\"key\":\"first-key\"}", 0));
        assertThat(keyClient.getPublicKey()).isEqualTo("first-key");
        assertThat(keyClient.getPublicKey()).isEqualTo("first-key");
        assertThat(REQUESTS).hasSize(1);
        RESPONSE.set(new Reply(200, "second-key", 0));
        assertThat(keyClient.refreshPublicKey()).isEqualTo("second-key");
        assertThat(keyClient.getPublicKey()).isEqualTo("second-key");
        assertThat(publicKeys.count()).isEqualTo(1);
        assertThat(publicKeys.findFirstByOrderByIdAsc().orElseThrow().getUpdatedAt()).isNotNull();
    }

    @Test
    void timeoutAndConnectionFailureDoNotReturnFakeProviderResponses() throws Exception {
        ReflectionTestUtils.setField(monoClient, "requestTimeout", Duration.ofMillis(40));
        RESPONSE.set(new Reply(200, "{}", 150));
        assertThatThrownBy(() -> monoClient.pay(invoice())).isInstanceOf(
                MonobankPaymentProviderException.class);
        String unreachable;
        try (var socket = new java.net.ServerSocket(0)) {
            unreachable = "http://127.0.0.1:" + socket.getLocalPort();
        }
        ReflectionTestUtils.setField(stripeClient, "baseUrl", unreachable);
        assertThatThrownBy(() -> stripeClient.getStatus(new StripeStatusRequest("missing"))).isInstanceOf(
                StripePaymentProviderException.class);
    }

    @Test
    void interruptedProviderCallPreservesThreadInterruptFlag() {
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> stripeClient.pay(checkout())).isInstanceOf(
                    StripePaymentProviderException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private MonobankInvoiceRequest invoice() {
        return new MonobankInvoiceRequest(2250L, 980,
                new MonobankInvoiceRequest.MerchantPaymInfo("123", "Dinner"),
                "https://front.example/result", "https://back.example/webhook", 900L,
                null, null, MonobankInvoiceRequest.PaymentType.debit);
    }

    private StripeCheckoutSessionRequest checkout() {
        return new StripeCheckoutSessionRequest(123L, 2250L, "eur", "Order + tips & dinner",
                "https://front.example/success?a=1&b=2", "https://front.example/cancel");
    }

    private record Reply(int status, String body, long delayMillis) {}

    private record RecordedRequest(String method, String path, Map<String, List<String>> headers, String body) {}
}
