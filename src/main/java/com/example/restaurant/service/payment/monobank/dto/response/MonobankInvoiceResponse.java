package com.example.restaurant.service.payment.monobank.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MonobankInvoiceResponse(
        String invoiceId,
        String pageUrl,
        String appUrl
) {}
