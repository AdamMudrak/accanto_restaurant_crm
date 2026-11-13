package com.example.restaurant.dto.request.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkInvoiceRequest(@NotBlank @Size(max = 255) String invoiceId) {}
