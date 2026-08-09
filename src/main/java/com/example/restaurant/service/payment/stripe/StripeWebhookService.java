package com.example.restaurant.service.payment.stripe;

import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.exception.types.unauthorized.InvalidTokenException;
import com.example.restaurant.repository.payment.PaymentRepository;
import com.example.restaurant.service.payment.PaymentOrderItemService;
import com.example.restaurant.service.payment.stripe.dto.request.StripeWebhookRequest;
import com.example.restaurant.service.payment.stripe.dto.response.StripeCheckoutSessionResponse;
import com.example.restaurant.service.utils.JsonConverter;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDateTime;
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
    private final PaymentOrderItemService paymentOrderItemService;
    @Value("${payment.stripe.webhook-secret}")
    private String webhookSecret;

    @Transactional
    public void handle(String rawBody, String signature) {
        verifySignature(rawBody, signature);

        StripeWebhookRequest webhook = jsonConverter.fromJson(rawBody, StripeWebhookRequest.class);
        if (!isSupportedEvent(webhook.type())) {
            return;
        }

        StripeCheckoutSessionResponse session = webhook.data().object();
        Payment payment = paymentRepository.findByProviderPaymentId(session.id())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Payment with stripe session id " + session.id() + " not found"));

        if (isOldWebhook(payment, webhook)) {
            return;
        }

        payment.setProviderModifiedAt(toOffsetDateTime(webhook.created()));
        applyStatus(payment, webhook.type(), session);

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
                && !toOffsetDateTime(webhook.created()).isAfter(payment.getProviderModifiedAt());
    }

    private boolean isSupportedEvent(String type) {
        return SESSION_COMPLETED.equals(type)
                || SESSION_EXPIRED.equals(type)
                || SESSION_ASYNC_SUCCEEDED.equals(type)
                || SESSION_ASYNC_FAILED.equals(type);
    }

    private void applyStatus(Payment payment, String type, StripeCheckoutSessionResponse session) {
        if ((SESSION_COMPLETED.equals(type) || SESSION_ASYNC_SUCCEEDED.equals(type))
                && "paid".equals(session.paymentStatus())) {
            payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
            payment.setCompletedAt(LocalDateTime.now());
            paymentOrderItemService.markItemsAsPaid(payment);
            return;
        }

        if (SESSION_EXPIRED.equals(type) || "expired".equals(session.status())) {
            payment.setStatus(Payment.PaymentStatus.EXPIRED);
            return;
        }

        if (SESSION_ASYNC_FAILED.equals(type)) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            return;
        }

        payment.setStatus(Payment.PaymentStatus.PENDING);
    }

    private OffsetDateTime toOffsetDateTime(Long created) {
        if (created == null) {
            return null;
        }

        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(created), ZoneOffset.UTC);
    }
}
