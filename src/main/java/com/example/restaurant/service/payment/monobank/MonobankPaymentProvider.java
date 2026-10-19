package com.example.restaurant.service.payment.monobank;

import static com.example.restaurant.service.payment.monobank.MonobankConstants.MONOBANK_BASE_URL;

import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.entity.payment.PaymentRefund;
import com.example.restaurant.exception.types.internalserver.MonobankPaymentProviderException;
import com.example.restaurant.service.payment.PaymentProvider;
import com.example.restaurant.service.payment.PaymentProviderType;
import com.example.restaurant.service.payment.ProviderInvoice;
import com.example.restaurant.service.payment.ProviderRefund;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonobankPaymentProvider implements PaymentProvider<
        MonobankInvoiceRequest, MonobankInvoiceResponse,
        MonobankStatusRequest, MonobankStatusResponse> {
    private static final String INVOICE_PATH = "/api/merchant/invoice/create";
    private static final String STATUS_PATH = "/api/merchant/invoice/status?invoiceId=";
    private static final int REQUEST_TIMEOUT_SECONDS = 15;
    private static final int HTTP_OK = 200;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS);
    private final JsonConverter jsonConverter;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    @Value("${payment.monobank.x-token}")
    private String monoXToken;
    @Value("${payment.monobank.base-url:https://api.monobank.ua}")
    private String baseUrl = MONOBANK_BASE_URL;
    @Value("${payment.provider.request-timeout:15s}")
    private Duration requestTimeout = REQUEST_TIMEOUT;

    @Override
    public PaymentProviderType getType() {
        return PaymentProviderType.MONOBANK;
    }

    @Override
    public MonobankInvoiceResponse pay(MonobankInvoiceRequest request) {

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + INVOICE_PATH))
                .timeout(requestTimeout)
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
        URI uri = URI.create(baseUrl + STATUS_PATH + encodedInvoiceId);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(requestTimeout)
                .header("X-Token", monoXToken)
                .GET()
                .build();

        HttpResponse<String> httpResponse = getOrThrow(httpRequest);
        throwIfNot200(httpResponse);

        return jsonConverter.fromJson(
                httpResponse.body(), MonobankStatusResponse.class);
    }

    @Override
    public ProviderInvoice inspect(String invoiceId) {
        var body = api("GET", STATUS_PATH + encode(invoiceId), null);
        var status = switch (String.valueOf(body.get("status"))) {
            case "success" -> Payment.PaymentStatus.SUCCEEDED;
            case "failure", "reversed" -> Payment.PaymentStatus.FAILED;
            case "expired" -> Payment.PaymentStatus.EXPIRED;
            case "created", "processing", "hold" -> Payment.PaymentStatus.PENDING;
            default -> throw new MonobankPaymentProviderException("Unknown invoice status.");
        };
        String currency = "980".equals(String.valueOf(body.get("ccy"))) ? "UAH" : null;
        Object modified = body.get("modifiedDate");
        return new ProviderInvoice(
                (String) body.get("invoiceId"), (String) body.get("reference"),
                body.get("amount") instanceof Number value ? value.longValue() : -1,
                currency, status, modified == null ? null
                        : OffsetDateTime.parse(modified.toString()));
    }

    @Override
    public ProviderRefund refund(
            String invoiceId, String reference, long amount, String currency) {
        api("POST", "/api/merchant/invoice/cancel", jsonConverter.toJson(Map.of(
                "invoiceId", invoiceId, "extRef", reference, "amount", amount)));
        // Accepted is not proof of successful return of funds.
        return inspectRefund(invoiceId, reference, reference, amount, currency);
    }

    @Override
    public ProviderRefund inspectRefund(
            String invoiceId, String reference, String refundId, long amount, String currency) {
        var body = api("GET", STATUS_PATH + encode(invoiceId), null);
        if (!invoiceId.equals(body.get("invoiceId"))) {
            throw new MonobankPaymentProviderException("Invoice ID does not match refund.");
        }
        if (body.get("cancelList") instanceof List<?> entries) {
            for (Object entry : entries) {
                if (entry instanceof Map<?, ?> cancel
                        && reference.equals(cancel.get("extRef"))) {
                    var status = switch (String.valueOf(cancel.get("status"))) {
                        case "success" -> PaymentRefund.Status.SUCCEEDED;
                        case "failure" -> PaymentRefund.Status.FAILED;
                        default -> PaymentRefund.Status.PENDING;
                    };
                    long returned = cancel.get("amount") instanceof Number value
                            ? value.longValue() : -1;
                    return new ProviderRefund(
                            reference, status, returned,
                            "980".equals(String.valueOf(cancel.get("ccy"))) ? "UAH" : null);
                }
            }
        }
        return new ProviderRefund(reference,
                PaymentRefund.Status.PENDING, amount, currency);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Map<?, ?> api(String method, String path, String data) {
        var request = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(requestTimeout)
                .header("X-Token", monoXToken).header("Content-Type", "application/json")
                .method(method, data == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(data)).build();
        var response = getOrThrow(request);
        throwIfNot200(response);
        return jsonConverter.fromJson(response.body(), Map.class);
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
