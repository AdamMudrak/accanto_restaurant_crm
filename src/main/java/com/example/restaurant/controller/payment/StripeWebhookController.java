package com.example.restaurant.controller.payment;

import com.example.restaurant.service.payment.stripe.StripeWebhookService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/stripe")
@RequiredArgsConstructor
@Hidden
public class StripeWebhookController {
    private final StripeWebhookService webhookService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader("Stripe-Signature") String signature) {
        webhookService.handle(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
