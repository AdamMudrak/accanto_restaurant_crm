package com.example.restaurant.dto.request.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolvePaymentRequest(@NotNull Action action,
                                    @NotBlank @Size(max = 2000) String evidence) {
    public enum Action { CONFIRM_NOT_CHARGED, ACKNOWLEDGE }
}
