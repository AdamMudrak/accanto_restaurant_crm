package com.example.restaurant.service.payment.stripe.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StripeCheckoutSessionResponse(
        String id,
        String url,
        String status,
        @JsonProperty("payment_status")
        String paymentStatus
) {}
