package com.example.restaurant.dto.request.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Verified online invoice recovery request.")
public record LinkInvoiceRequest(
        @Schema(description = "Existing provider invoice ID to verify and link.",
                example = "cs_example")
        @NotBlank @Size(max = 255) String invoiceId) {}
