package com.example.restaurant.service.payment.monobank;

import static com.example.restaurant.service.payment.monobank.MonobankConstants.MONOBANK_BASE_URL;

import com.example.restaurant.exception.types.internalserver.MonobankPaymentProviderException;
import com.example.restaurant.service.payment.PaymentProvider;
import com.example.restaurant.service.payment.PaymentProviderType;
import com.example.restaurant.service.payment.monobank.dto.request.MonobankInvoiceRequest;
import com.example.restaurant.service.payment.monobank.dto.request.MonobankStatusRequest;
import com.example.restaurant.service.payment.monobank.dto.response.MonobankInvoiceResponse;
import com.example.restaurant.service.payment.monobank.dto.response.MonobankStatusResponse;
import com.example.restaurant.service.utils.JsonConverter;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonobankPaymentProvider implements PaymentProvider<
        MonobankInvoiceRequest, MonobankInvoiceResponse,
        MonobankStatusRequest, MonobankStatusResponse> {
    private static final String INVOICE_URL = MONOBANK_BASE_URL + "/api/merchant/invoice/create";
    private static final String STATUS_URL = MONOBANK_BASE_URL
            + "/api/merchant/invoice/status?invoiceId=";
    private static final int REQUEST_TIMEOUT_SECONDS = 15;
    private static final int HTTP_OK = 200;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS);
    private final JsonConverter jsonConverter;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    @Value("${payment.monobank.x-token}")
    private String monoXToken;

    @Override
    public PaymentProviderType getType() {
        return PaymentProviderType.MONOBANK;
    }

    @Override
    public MonobankInvoiceResponse pay(MonobankInvoiceRequest request) {

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(INVOICE_URL))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("X-Token", monoXToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonConverter.toJson(request)))
                .build();

        HttpResponse<String> httpResponse = getOrThrow(httpRequest);
        throwIfNot200(httpResponse);

        return jsonConverter.fromJson(
                httpResponse.body(), MonobankInvoiceResponse.class);
    }

    @Override
    public MonobankStatusResponse getStatus(MonobankStatusRequest request) {
        String encodedInvoiceId = URLEncoder.encode(request.invoiceId(), StandardCharsets.UTF_8);
        URI uri = URI.create(STATUS_URL + encodedInvoiceId);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("X-Token", monoXToken)
                .GET()
                .build();

        HttpResponse<String> httpResponse = getOrThrow(httpRequest);
        throwIfNot200(httpResponse);

        return jsonConverter.fromJson(
                httpResponse.body(), MonobankStatusResponse.class);
    }

    private HttpResponse<String> getOrThrow(HttpRequest httpRequest) {
        try {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new MonobankPaymentProviderException(ie.getMessage());
        } catch (IOException ioe) {
            throw new MonobankPaymentProviderException(ioe.getMessage());
        }
    }

    private void throwIfNot200(HttpResponse<String> httpResponse) {
        if (httpResponse.statusCode() != HTTP_OK) {
            throw new MonobankPaymentProviderException(
                    httpResponse.statusCode(), httpResponse.body());
        }
    }
}
