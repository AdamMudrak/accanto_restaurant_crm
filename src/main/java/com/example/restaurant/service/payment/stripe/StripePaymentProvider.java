package com.example.restaurant.service.payment.stripe;

import com.example.restaurant.exception.types.internalserver.StripePaymentProviderException;
import com.example.restaurant.service.payment.PaymentProvider;
import com.example.restaurant.service.payment.PaymentProviderType;
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
    private static final String CHECKOUT_SESSION_URL =
            "https://api.stripe.com/v1/checkout/sessions";
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

    @Override
    public PaymentProviderType getType() {
        return PaymentProviderType.STRIPE;
    }

    @Override
    public StripeCheckoutSessionResponse pay(StripeCheckoutSessionRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(CHECKOUT_SESSION_URL))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
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
                .uri(URI.create(CHECKOUT_SESSION_URL + "/" + encodedSessionId))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + secretKey)
                .GET()
                .build();

        HttpResponse<String> httpResponse = getOrThrow(httpRequest);
        throwIfNotSuccessful(httpResponse);

        return jsonConverter.fromJson(httpResponse.body(), StripeCheckoutSessionResponse.class);
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
