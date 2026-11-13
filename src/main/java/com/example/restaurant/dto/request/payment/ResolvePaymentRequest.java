package com.example.restaurant.dto.request.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Evidence-based resolution of an existing reconciliation issue.")
public record ResolvePaymentRequest(
        @Schema(description = "CONFIRM_NOT_CHARGED for unknown invoices; ACKNOWLEDGE after "
                    + "confirmed refund.",
                example = "CONFIRM_NOT_CHARGED")
        @NotNull Action action,

        @Schema(description = "Provider dashboard evidence supporting the resolution, up to "
                    + "2000 characters.",
                example = "Provider dashboard confirms no charge for payment 42.")
        @NotBlank @Size(max = 2000) String evidence) {
    public enum Action { CONFIRM_NOT_CHARGED, ACKNOWLEDGE }
}
