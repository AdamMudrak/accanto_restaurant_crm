package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.order.OrderItem;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.node.NullNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

class WebhookIntegrationTest extends AbstractIntegrationTest {
    private static final KeyPair MONO_KEYS = monoKeyPair();

    @ParameterizedTest
    @CsvSource({"created,PENDING", "processing,PENDING", "hold,PENDING",
            "success,SUCCEEDED", "failure,FAILED", "reversed,FAILED", "expired,EXPIRED"})
    void signedMonobankEventsUpdatePaymentAndOrderState(String providerStatus, Payment.PaymentStatus expected)
            throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long id = addOnlinePayment(fixture, Payment.PaymentStatus.PENDING);
        when(monoKeys.getPublicKey()).thenReturn(encodedPublicKey(MONO_KEYS));
        String body = monoEvent(id, providerStatus, OffsetDateTime.now(ZoneOffset.UTC));
        assertThat(monoWebhook(body, body, MONO_KEYS).status()).isEqualTo(200);
        assertThat(paymentService.getPaymentById(id).status()).isEqualTo(expected);
        assertThat(items.findById(fixture.itemId()).orElseThrow().getStatus())
                .isEqualTo(expected == Payment.PaymentStatus.SUCCEEDED
                        ? OrderItem.OrderItemStatus.PAID_CARD : OrderItem.OrderItemStatus.CONFIRMED);
    }

    @Test
    void monobankKeyRotationRetriesSignatureWithRefreshedPublicKey() throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long id = addOnlinePayment(fixture, Payment.PaymentStatus.PENDING);
        when(monoKeys.getPublicKey()).thenReturn(encodedPublicKey(monoKeyPair()));
        when(monoKeys.refreshPublicKey()).thenReturn(encodedPublicKey(MONO_KEYS));
        String body = monoEvent(id, "success", OffsetDateTime.now(ZoneOffset.UTC));
        assertThat(monoWebhook(body, body, MONO_KEYS).status()).isEqualTo(200);
        Mockito.verify(monoKeys).refreshPublicKey();
        assertThat(paymentService.getPaymentById(id).status()).isEqualTo(Payment.PaymentStatus.SUCCEEDED);
    }

    @Test
    void tamperedMonobankEventIsRejectedAndDoesNotSettlePayment() throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long id = addOnlinePayment(fixture, Payment.PaymentStatus.PENDING);
        when(monoKeys.getPublicKey()).thenReturn(encodedPublicKey(MONO_KEYS));
        when(monoKeys.refreshPublicKey()).thenReturn(encodedPublicKey(MONO_KEYS));
        String signed = monoEvent(id, "created", OffsetDateTime.now(ZoneOffset.UTC));
        assertThat(monoWebhook(signed.replace("created", "success"), signed, MONO_KEYS).status()).isEqualTo(401);
        assertThat(paymentService.getPaymentById(id).status()).isEqualTo(Payment.PaymentStatus.PENDING);
    }

    @Test
    void malformedMonobankSignatureIsUnauthorizedAndMissingSignatureIsBadRequest() throws Exception {
        when(monoKeys.getPublicKey()).thenReturn(encodedPublicKey(MONO_KEYS));
        when(monoKeys.refreshPublicKey()).thenReturn(encodedPublicKey(MONO_KEYS));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/payments/monobank/webhook"))
                .header("Content-Type", "application/json").header("x-sign", "not-base64")
                .POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        assertThat(HTTP.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(401);
        assertThat(request("POST", "/payments/monobank/webhook", "{}", null).status()).isEqualTo(400);
    }

    @ParameterizedTest
    @CsvSource({"checkout.session.expired,expired,unpaid,EXPIRED",
            "checkout.session.async_payment_failed,complete,unpaid,FAILED",
            "checkout.session.async_payment_succeeded,complete,paid,SUCCEEDED",
            "checkout.session.completed,complete,unpaid,PENDING"})
    void stripeEventTypesMapToExpectedStates(String type, String state, String paid, Payment.PaymentStatus expected)
            throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.EUR);
        Long id = addOnlinePayment(fixture, Payment.PaymentStatus.PENDING);
        long timestamp = Instant.now().getEpochSecond();
        String invoice = paymentService.getPaymentById(id).invoiceId();
        String body = stripeEvent(invoice, timestamp, paid)
                .replace("checkout.session.completed", type).replace("\"status\":\"complete\"", "\"status\":\"" + state + "\"");
        assertThat(stripeWebhook(body, timestamp, "test")).isEqualTo(200);
        assertThat(paymentService.getPaymentById(id).status()).isEqualTo(expected);
    }

    @Test
    void unsupportedStripeEventIsAcknowledgedWithoutLookingUpOrChangingPayment() throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String body = stripeEvent("missing-session", timestamp, "paid")
                .replace("checkout.session.completed", "customer.created");
        assertThat(stripeWebhook(body, timestamp, "test")).isEqualTo(200);
        assertThat(payments.count()).isZero();
    }

    @Test
    void oldProviderEventCannotOverwriteNewerStateAndSuccessCannotBeDowngraded() throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long id = addOnlinePayment(fixture, Payment.PaymentStatus.PENDING);
        when(monoKeys.getPublicKey()).thenReturn(encodedPublicKey(MONO_KEYS));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String processing = monoEvent(id, "processing", now);
        assertThat(monoWebhook(processing, processing, MONO_KEYS).status()).isEqualTo(200);
        String old = monoEvent(id, "success", now.minusMinutes(1));
        assertThat(monoWebhook(old, old, MONO_KEYS).status()).isEqualTo(200);
        assertThat(paymentService.getPaymentById(id).status()).isEqualTo(Payment.PaymentStatus.PENDING);
        String success = monoEvent(id, "success", now.plusSeconds(1));
        assertThat(monoWebhook(success, success, MONO_KEYS).status()).isEqualTo(200);
        String reversed = monoEvent(id, "reversed", now.plusSeconds(2));
        assertThat(monoWebhook(reversed, reversed, MONO_KEYS).status()).isEqualTo(200);
        assertThat(paymentService.getPaymentById(id).status()).isEqualTo(Payment.PaymentStatus.SUCCEEDED);
        assertThat(paymentService.getPaymentById(id).reconciliationIssue()).contains("manual reconciliation");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1000, 1000})
    void staleOrFutureStripeSignaturesAreRejected(long offset) throws Exception {
        long timestamp = Instant.now().getEpochSecond() + offset;
        assertThat(stripeWebhook(stripeEvent("missing-session", timestamp, "paid"), timestamp, "test"))
                .isEqualTo(401);
    }

    @Test
    void unknownInvoiceReturnsNotFoundAndSignedMalformedPayloadReturnsBadRequest() throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        assertThat(stripeWebhook(stripeEvent("missing-session", timestamp, "paid"), timestamp, "test"))
                .isEqualTo(404);
        assertThat(stripeWebhook("{", timestamp, "test")).isEqualTo(400);
    }

    private static KeyPair monoKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(256);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String encodedPublicKey(KeyPair keys) {
        String pem = "-----BEGIN PUBLIC KEY-----\n" + Base64.getEncoder()
                .encodeToString(keys.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----";
        return Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));
    }

    private String monoEvent(Long id, String state, OffsetDateTime modified) throws Exception {
        return json.writeValueAsString(Map.of("invoiceId", paymentService.getPaymentById(id).invoiceId(),
                "status", state, "modifiedDate", modified.toString()));
    }

    private ApiResult monoWebhook(String body, String signedBody, KeyPair keys) throws Exception {
        var signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(keys.getPrivate());
        signer.update(signedBody.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signer.sign());
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/payments/monobank/webhook"))
                .header("Content-Type", "application/json").header("x-sign", signature)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
        return new ApiResult(response.statusCode(), NullNode.getInstance(),
                response.body(), response.headers());
    }
    @Test
    void signedStripeWebhookSettlesPaymentAndSameTimestampEventsDoNotLoseSuccess() throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.EUR);
        Long id = addOnlinePayment(fixture, Payment.PaymentStatus.PENDING);
        String invoice = payments.findById(id).orElseThrow().getProviderPaymentId();
        long timestamp = Instant.now().getEpochSecond();
        String unpaid = stripeEvent(invoice, timestamp, "unpaid");
        String paid = stripeEvent(invoice, timestamp, "paid");
        assertThat(stripeWebhook(unpaid, timestamp, "wrong-secret")).isEqualTo(401);
        assertThat(stripeWebhook(unpaid, timestamp, "test")).isEqualTo(200);
        assertThat(stripeWebhook(paid, timestamp, "test")).isEqualTo(200);
        LocalDateTime completed = payments.findById(id).orElseThrow().getCompletedAt();
        assertThat(stripeWebhook(paid, timestamp, "test")).isEqualTo(200);
        assertThat(payments.findById(id).orElseThrow().getCompletedAt()).isEqualTo(completed);
        assertThat(payments.findById(id).orElseThrow().getStatus())
                .isEqualTo(Payment.PaymentStatus.SUCCEEDED);
        assertThat(items.findById(fixture.itemId()).orElseThrow().getStatus())
                .isEqualTo(OrderItem.OrderItemStatus.PAID_CARD);
    }

    private String stripeEvent(String invoice, long timestamp, String paymentStatus) {
        return "{\"id\":\"evt_test\",\"type\":\"checkout.session.completed\",\"created\":"
                + timestamp + ",\"data\":{\"object\":{\"id\":\"" + invoice
                + "\",\"status\":\"complete\",\"payment_status\":\"" + paymentStatus + "\"}}}";
    }

    private int stripeWebhook(String body, long timestamp, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + body)
                .getBytes(StandardCharsets.UTF_8)));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + "/payments/stripe/webhook"))
                .header("Content-Type", "application/json")
                .header("Stripe-Signature", "t=" + timestamp + ",v1=" + signature)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return HTTP.send(request,
                HttpResponse.BodyHandlers.discarding()).statusCode();
    }

}
