package com.example.restaurant.service.payment.monobank.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MonobankPublicKeyResponse(String key) {
}
