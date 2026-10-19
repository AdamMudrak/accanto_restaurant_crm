package com.example.restaurant.service.payment.stripe;

import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.entity.payment.PaymentRefund;
import com.example.restaurant.exception.types.internalserver.StripePaymentProviderException;
import com.example.restaurant.service.payment.PaymentProvider;
import com.example.restaurant.service.payment.PaymentProviderType;
import com.example.restaurant.service.payment.ProviderInvoice;
import com.example.restaurant.service.payment.ProviderRefund;
import com.example.restaurant.service.payment.stripe.dto.request.StripeCheckoutSessionRequest;
import com.example.restaurant.service.payment.stripe.dto.request.StripeStatusRequest;
import com.example.restaurant.service.payment.stripe.dto.response.StripeCheckoutSessionResponse;
import com.example.restaurant.service.utils.JsonConverter;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StripePaymentProvider implements PaymentProvider<
        StripeCheckoutSessionRequest, StripeCheckoutSessionResponse,
        StripeStatusRequest, StripeCheckoutSessionResponse> {
    private static final String CHECKOUT_SESSION_PATH = "/v1/checkout/sessions";
    private static final int REQUEST_TIMEOUT_SECONDS = 15;
    private static final int MIN_SUCCESSFUL_HTTP_STATUS = 200;
    private static final int MAX_SUCCESSFUL_HTTP_STATUS_EXCLUSIVE = 300;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS);
    private final JsonConverter jsonConverter;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    @Value("${payment.stripe.secret-key}")
    private String secretKey;
    @Value("${payment.stripe.base-url:https://api.stripe.com}")
    private String baseUrl = "https://api.stripe.com";
    @Value("${payment.provider.request-timeout:15s}")
    private Duration requestTimeout = REQUEST_TIMEOUT;

    @Override
    public PaymentProviderType getType() {
        return PaymentProviderType.STRIPE;
    }

    @Override
    public StripeCheckoutSessionResponse pay(StripeCheckoutSessionRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + CHECKOUT_SESSION_PATH))
                .timeout(requestTimeout)
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Idempotency-Key", "restaurant-payment-" + request.paymentId())
                .POST(HttpRequest.BodyPublishers.ofString(toFormData(request)))
                .build();

        HttpResponse<String> httpResponse = getOrThrow(httpRequest);
        throwIfNotSuccessful(httpResponse);

        return jsonConverter.fromJson(httpResponse.body(), StripeCheckoutSessionResponse.class);
    }

    @Override
    public StripeCheckoutSessionResponse getStatus(StripeStatusRequest request) {
        String encodedSessionId = URLEncoder.encode(request.sessionId(), StandardCharsets.UTF_8);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + CHECKOUT_SESSION_PATH + "/" + encodedSessionId))
                .timeout(requestTimeout)
                .header("Authorization", "Bearer " + secretKey)
                .GET()
                .build();

        HttpResponse<String> httpResponse = getOrThrow(httpRequest);
        throwIfNotSuccessful(httpResponse);

        return jsonConverter.fromJson(httpResponse.body(), StripeCheckoutSessionResponse.class);
    }

    @Override
    public ProviderInvoice inspect(String invoiceId) {
        var body = api("GET", CHECKOUT_SESSION_PATH + "/" + encode(invoiceId), null, null);
        String paymentStatus = text(body, "payment_status");
        var status = "paid".equals(paymentStatus)
                ? Payment.PaymentStatus.SUCCEEDED
                : "expired".equals(text(body, "status"))
                ? Payment.PaymentStatus.EXPIRED
                : Payment.PaymentStatus.PENDING;
        return new ProviderInvoice(text(body, "id"),
                text(body, "client_reference_id"), number(body, "amount_total"),
                text(body, "currency"), status, null);
    }

    @Override
    public ProviderRefund refund(
            String invoiceId, String reference, long amount, String currency) {
        var session = api("GET", CHECKOUT_SESSION_PATH + "/" + encode(invoiceId), null, null);
        String intent = text(session, "payment_intent");
        if (intent == null || intent.isBlank()) {
            throw new StripePaymentProviderException("Checkout has no payment intent.");
        }
        return refundResult(api("POST", "/v1/refunds", "payment_intent=" + encode(intent)
                + "&amount=" + amount + "&metadata[reference]=" + encode(reference), reference));
    }

    @Override
    public ProviderRefund inspectRefund(
            String invoiceId, String reference, String refundId, long amount, String currency) {
        if (refundId == null) {
            // Never recreate an unknown refund after Stripe's idempotency retention window.
            throw new StripePaymentProviderException(
                    "Unknown refund result: verify Stripe dashboard.");
        }
        var session = api("GET", CHECKOUT_SESSION_PATH + "/" + encode(invoiceId), null, null);
        var refund = api("GET", "/v1/refunds/" + encode(refundId), null, null);
        if (text(session, "payment_intent") == null
                || !text(session, "payment_intent").equals(text(refund, "payment_intent"))) {
            throw new StripePaymentProviderException("Refund belongs to another checkout.");
        }
        return refundResult(refund);
    }

    private ProviderRefund refundResult(
            Map<?, ?> body) {
        var status = switch (String.valueOf(body.get("status"))) {
            case "succeeded" -> PaymentRefund.Status.SUCCEEDED;
            case "failed", "canceled" -> PaymentRefund.Status.FAILED;
            default -> PaymentRefund.Status.PENDING;
        };
        return new ProviderRefund(text(body, "id"),
                status, number(body, "amount"), text(body, "currency"));
    }

    private Map<?, ?> api(String method, String path, String data, String key) {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(requestTimeout)
                .header("Authorization", "Bearer " + secretKey);
        if (key != null) {
            builder.header("Idempotency-Key", key);
        }
        builder.header("Content-Type", "application/x-www-form-urlencoded");
        builder.method(method, data == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(data));
        var response = getOrThrow(builder.build());
        throwIfNotSuccessful(response);
        return jsonConverter.fromJson(response.body(), Map.class);
    }

    private String text(Map<?, ?> body, String key) {
        return body.get(key) instanceof String value ? value : null;
    }

    private long number(Map<?, ?> body, String key) {
        return body.get(key) instanceof Number value ? value.longValue() : -1;
    }

    private String toFormData(StripeCheckoutSessionRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mode", "payment");
        params.put("success_url", request.successUrl());
        params.put("cancel_url", request.cancelUrl());
        params.put("client_reference_id", String.valueOf(request.paymentId()));
        params.put("line_items[0][price_data][currency]", request.currency());
        params.put("line_items[0][price_data][unit_amount]", String.valueOf(request.amount()));
        params.put("line_items[0][price_data][product_data][name]", request.productName());
        params.put("line_items[0][quantity]", "1");
        params.put("metadata[paymentId]", String.valueOf(request.paymentId()));
        params.put("payment_intent_data[metadata][paymentId]", String.valueOf(request.paymentId()));

        return params.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private HttpResponse<String> getOrThrow(HttpRequest httpRequest) {
        try {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new StripePaymentProviderException(ie.getMessage());
        } catch (IOException ioe) {
            throw new StripePaymentProviderException(ioe.getMessage());
        }
    }

    private void throwIfNotSuccessful(HttpResponse<String> httpResponse) {
        if (httpResponse.statusCode() < MIN_SUCCESSFUL_HTTP_STATUS
                || httpResponse.statusCode() >= MAX_SUCCESSFUL_HTTP_STATUS_EXCLUSIVE) {
            throw new StripePaymentProviderException(
                    httpResponse.statusCode(), httpResponse.body());
        }
    }
}
