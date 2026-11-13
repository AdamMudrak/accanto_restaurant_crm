package com.example.restaurant.dto.request.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundRequest(@NotBlank @Size(max = 1000) String reason,
                            Boolean confirmCashReturned) {}
