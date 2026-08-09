package com.example.restaurant.service.payment.stripe.dto.request;

import com.example.restaurant.service.payment.stripe.dto.response.StripeCheckoutSessionResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StripeWebhookRequest(
        String id,
        String type,
        Long created,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            StripeCheckoutSessionResponse object
    ) {}
}
