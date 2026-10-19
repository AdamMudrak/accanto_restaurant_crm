package com.example.restaurant.service.payment.monobank;

import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.exception.types.badrequest.MalformedWebhookException;
import com.example.restaurant.exception.types.unauthorized.InvalidTokenException;
import com.example.restaurant.repository.payment.PaymentRepository;
import com.example.restaurant.service.payment.PaymentSettlementService;
import com.example.restaurant.service.payment.monobank.dto.request.MonobankWebhookRequest;
import com.example.restaurant.service.utils.JsonConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MonobankWebhookService {
    private final PaymentRepository paymentRepository;
    private final MonobankPublicKeyService publicKeyService;
    private final MonobankWebhookSignatureVerifier signatureVerifier;
    private final JsonConverter jsonConverter;
    private final PaymentSettlementService settlementService;

    @Transactional
    public void handle(String rawBody, String signature) {
        verifySignature(rawBody, signature);

        MonobankWebhookRequest webhook;
        try {
            webhook = jsonConverter.fromJson(rawBody, MonobankWebhookRequest.class);
        } catch (IllegalArgumentException exception) {
            throw new MalformedWebhookException();
        }
        if (webhook == null || webhook.invoiceId() == null || webhook.invoiceId().isBlank()
                || webhook.status() == null) {
            throw new MalformedWebhookException();
        }
        Payment payment = settlementService.lockByProviderId(webhook.invoiceId());

        if (isOldWebhook(payment, webhook)) {
            return;
        }

        settlementService.applyStatus(payment, toPaymentStatus(webhook.status()),
                webhook.modifiedDate());
        payment.setFailureReason(webhook.failureReason());
        payment.setProviderErrorCode(webhook.errCode());
        payment.setProviderErrorText(webhook.errText());

        paymentRepository.save(payment);
    }

    private void verifySignature(String rawBody, String signature) {
        String publicKey = publicKeyService.getPublicKey();
        boolean valid = signatureVerifier.verify(rawBody, signature, publicKey);

        if (!valid) {
            publicKey = publicKeyService.refreshPublicKey();
            valid = signatureVerifier.verify(rawBody, signature, publicKey);
        }

        if (!valid) {
            throw new InvalidTokenException("Invalid monobank webhook signature");
        }
    }

    private boolean isOldWebhook(Payment payment, MonobankWebhookRequest webhook) {
        return payment.getProviderModifiedAt() != null
                && webhook.modifiedDate() != null
                && webhook.modifiedDate().isBefore(payment.getProviderModifiedAt());
    }

    private Payment.PaymentStatus toPaymentStatus(MonobankWebhookRequest.Status status) {
        return switch (status) {
            case created, processing, hold -> Payment.PaymentStatus.PENDING;
            case success -> Payment.PaymentStatus.SUCCEEDED;
            case failure, reversed -> Payment.PaymentStatus.FAILED;
            case expired -> Payment.PaymentStatus.EXPIRED;
        };
    }
}
