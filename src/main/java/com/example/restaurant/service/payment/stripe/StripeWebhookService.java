package com.example.restaurant.service.payment.stripe;

import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.exception.types.badrequest.MalformedWebhookException;
import com.example.restaurant.exception.types.unauthorized.InvalidTokenException;
import com.example.restaurant.repository.payment.PaymentRepository;
import com.example.restaurant.service.payment.PaymentSettlementService;
import com.example.restaurant.service.payment.stripe.dto.request.StripeWebhookRequest;
import com.example.restaurant.service.payment.stripe.dto.response.StripeCheckoutSessionResponse;
import com.example.restaurant.service.utils.JsonConverter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StripeWebhookService {
    private static final String SESSION_COMPLETED = "checkout.session.completed";
    private static final String SESSION_EXPIRED = "checkout.session.expired";
    private static final String SESSION_ASYNC_SUCCEEDED =
            "checkout.session.async_payment_succeeded";
    private static final String SESSION_ASYNC_FAILED = "checkout.session.async_payment_failed";
    private final PaymentRepository paymentRepository;
    private final StripeWebhookSignatureVerifier signatureVerifier;
    private final JsonConverter jsonConverter;
    private final PaymentSettlementService settlementService;
    @Value("${payment.stripe.webhook-secret}")
    private String webhookSecret;

    @Transactional
    public void handle(String rawBody, String signature) {
        verifySignature(rawBody, signature);

        StripeWebhookRequest webhook;
        try {
            webhook = jsonConverter.fromJson(rawBody, StripeWebhookRequest.class);
        } catch (IllegalArgumentException exception) {
            throw new MalformedWebhookException();
        }
        if (webhook == null || webhook.type() == null) {
            throw new MalformedWebhookException();
        }
        if (!isSupportedEvent(webhook.type())) {
            return;
        }

        if (webhook.data() == null) {
            throw new MalformedWebhookException();
        }
        StripeCheckoutSessionResponse session = webhook.data().object();
        if (session == null || session.id() == null || session.id().isBlank()) {
            throw new MalformedWebhookException();
        }
        Payment payment = settlementService.lockByProviderId(session.id());

        if (isOldWebhook(payment, webhook)) {
            return;
        }

        settlementService.applyStatus(payment, toPaymentStatus(webhook.type(), session),
                toOffsetDateTime(webhook.created()));

        paymentRepository.save(payment);
    }

    private void verifySignature(String rawBody, String signature) {
        boolean valid = signatureVerifier.verify(rawBody, signature, webhookSecret);

        if (!valid) {
            throw new InvalidTokenException("Invalid stripe webhook signature");
        }
    }

    private boolean isOldWebhook(Payment payment, StripeWebhookRequest webhook) {
        return payment.getProviderModifiedAt() != null
                && webhook.created() != null
                && toOffsetDateTime(webhook.created()).isBefore(payment.getProviderModifiedAt());
    }

    private boolean isSupportedEvent(String type) {
        return SESSION_COMPLETED.equals(type)
                || SESSION_EXPIRED.equals(type)
                || SESSION_ASYNC_SUCCEEDED.equals(type)
                || SESSION_ASYNC_FAILED.equals(type);
    }

    private Payment.PaymentStatus toPaymentStatus(String type,
                                                  StripeCheckoutSessionResponse session) {
        if ((SESSION_COMPLETED.equals(type) || SESSION_ASYNC_SUCCEEDED.equals(type))
                && "paid".equals(session.paymentStatus())) {
            return Payment.PaymentStatus.SUCCEEDED;
        }

        if (SESSION_EXPIRED.equals(type) || "expired".equals(session.status())) {
            return Payment.PaymentStatus.EXPIRED;
        }

        if (SESSION_ASYNC_FAILED.equals(type)) {
            return Payment.PaymentStatus.FAILED;
        }

        return Payment.PaymentStatus.PENDING;
    }

    private OffsetDateTime toOffsetDateTime(Long created) {
        if (created == null) {
            return null;
        }

        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(created), ZoneOffset.UTC);
    }
}
