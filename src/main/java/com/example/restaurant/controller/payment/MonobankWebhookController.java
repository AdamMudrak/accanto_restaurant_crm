package com.example.restaurant.controller.payment;

import com.example.restaurant.service.payment.monobank.MonobankWebhookService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/monobank")
@RequiredArgsConstructor
@Hidden
public class MonobankWebhookController {
    private final MonobankWebhookService webhookService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader("x-sign") String signature
    ) {
        webhookService.handle(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
