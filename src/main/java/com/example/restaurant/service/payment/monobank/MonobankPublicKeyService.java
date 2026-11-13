package com.example.restaurant.service.payment.monobank;

import com.example.restaurant.entity.payment.MonobankPublicKey;
import com.example.restaurant.exception.types.internalserver.MonobankPaymentProviderException;
import com.example.restaurant.repository.payment.MonobankPublicKeyRepository;
import com.example.restaurant.service.payment.monobank.dto.response.MonobankPublicKeyResponse;
import com.example.restaurant.service.utils.JsonConverter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MonobankPublicKeyService {
    private static final String PUBKEY_PATH = "/api/merchant/pubkey";
    private static final int REQUEST_TIMEOUT_SECONDS = 15;
    private static final int HTTP_OK = 200;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS);
    private final MonobankPublicKeyRepository publicKeyRepository;
    private final JsonConverter jsonConverter;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    @Value("${payment.monobank.x-token}")
    private String monoXToken;
    @Value("${payment.monobank.base-url:https://api.monobank.ua}")
    private String baseUrl = MonobankConstants.MONOBANK_BASE_URL;
    @Value("${payment.provider.request-timeout:15s}")
    private Duration requestTimeout = REQUEST_TIMEOUT;

    @Transactional
    public String getPublicKey() {
        return publicKeyRepository.findFirstByOrderByIdAsc()
                .map(MonobankPublicKey::getPublicKey)
                .orElseGet(this::refreshPublicKey);
    }

    @Transactional
    public String refreshPublicKey() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + PUBKEY_PATH))
                .timeout(requestTimeout)
                .header("X-Token", monoXToken)
                .GET()
                .build();

        HttpResponse<String> response = getOrThrow(request);
        throwIfNot200(response);

        String publicKey = extractPublicKey(response.body());
        MonobankPublicKey keyEntity = publicKeyRepository.findFirstByOrderByIdAsc()
                .orElseGet(MonobankPublicKey::new);
        keyEntity.setPublicKey(publicKey);
        keyEntity.setUpdatedAt(LocalDateTime.now());
        publicKeyRepository.save(keyEntity);

        return publicKey;
    }

    private String extractPublicKey(String responseBody) {
        try {
            MonobankPublicKeyResponse response = jsonConverter.fromJson(
                    responseBody, MonobankPublicKeyResponse.class);
            if (response.key() != null && !response.key().isBlank()) {
                return response.key();
            }
        } catch (IllegalArgumentException ignored) {
            // Some test environments can provide the key directly instead of JSON.
        }

        return responseBody.replace("\"", "").trim();
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
