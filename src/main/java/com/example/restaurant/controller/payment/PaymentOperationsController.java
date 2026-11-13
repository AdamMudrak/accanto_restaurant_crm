package com.example.restaurant.controller.payment;

import com.example.restaurant.dto.request.payment.LinkInvoiceRequest;
import com.example.restaurant.dto.request.payment.RefundRequest;
import com.example.restaurant.dto.request.payment.ResolvePaymentRequest;
import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.dto.response.payment.PaymentResponse;
import com.example.restaurant.dto.response.payment.RefundResponse;
import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.service.payment.PaymentOperationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/{id}")
@PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
@RequiredArgsConstructor
@Tag(name = "Payment operations", description = "Administrator invoice recovery, reconciliation, "
        + "and full receipt refunds. All routes require ADMIN_EMPLOYEE access.")
@SecurityRequirement(name = "accessTokenCookie")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                description = "Operation completed; inspect the returned payment or refund status.",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                description = "Invalid request body, identifier, or parameter."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                description = "Missing authentication or administrator access."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                description = "Payment or refund was not found."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                description = "Receipt, invoice, or refund state conflicts with this operation.")
})
public class PaymentOperationsController {
    private final PaymentOperationsService operations;

    @Operation(summary = "Recover and verify a provider invoice",
            description = "Online payments only. Verifies reference, amount including tips, "
                    + "currency, and "
                    + "provider status. Cannot replace a linked invoice or reuse another payment "
                    + "invoice. Applies provider status and records the actor; does not create a "
                    + "charge.")
    @PostMapping("/invoice")
    public ApiResponse<PaymentResponse> link(
            @Parameter(description = "Original payment ID, not refund ID.", example = "42")
            @PathVariable @Positive Long id,
            @Valid @RequestBody LinkInvoiceRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Employee actor) {
        return ApiResponse.success("Provider invoice verified.", operations.link(id, request,
                actor));
    }

    @Operation(summary = "Resolve a payment reconciliation issue",
            description = "Requires an unresolved issue and evidence. CONFIRM_NOT_CHARGED "
                    + "marks a pending "
                    + "payment without an invoice as failed after checking the provider dashboard. "
                    + "ACKNOWLEDGE requires a final payment status and a successful refund. "
                    + "Records "
                    + "the actor and evidence.")
    @PostMapping("/resolution")
    public ApiResponse<PaymentResponse> resolve(@Parameter(description = "Original payment "
                    + "ID, not refund ID.", example = "42")
            @PathVariable @Positive Long id,
            @Valid @RequestBody ResolvePaymentRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Employee actor) {
        return ApiResponse.success("Payment issue resolved.", operations.resolve(id, request,
                actor));
    }

    @Operation(summary = "Refund a full receipt including tips",
            description = "Requires a successful, non-reconstructed receipt with known "
                    + "currency. Reverse "
                    + "tip payouts first. Cash requires confirmCashReturned=true and records cash "
                    + "already returned. Online requires a linked invoice. Repeated "
                    + "requests return "
                    + "the existing refund. HTTP 200 does not guarantee completion: inspect status "
                    + "and error. Synchronize UNKNOWN outcomes before retrying.")
    @PostMapping("/refund")
    public ApiResponse<RefundResponse> refund(@Parameter(description = "Original payment "
                    + "ID, not refund ID.", example = "42")
            @PathVariable @Positive Long id,
            @Valid @RequestBody RefundRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Employee actor) {
        return ApiResponse.success("Refund recorded.", operations.refund(id, request, actor));
    }

    @Operation(summary = "Retry a provider-confirmed failed refund",
            description = "Only FAILED refunds can be retried. Creates a new attempt for "
                    + "the same full "
                    + "receipt. Synchronize PENDING or UNKNOWN outcomes instead to avoid duplicate "
                    + "returns. Inspect the returned status and error.")
    @PostMapping("/refund/retry")
    public ApiResponse<RefundResponse> retry(
            @Parameter(description = "Original payment ID, not refund ID.", example = "42")
            @PathVariable @Positive Long id) {
        return ApiResponse.success("Refund retry recorded.", operations.retryRefund(id));
    }

    @Operation(summary = "Synchronize a refund with its provider",
            description = "Inspects an existing refund without requesting another return. Verifies "
                    + "amount, currency, and refund ID. Final successful or failed refunds remain "
                    + "unchanged. Provider errors produce UNKNOWN status and an error in "
                    + "the HTTP 200 "
                    + "response.")
    @PostMapping("/refund/sync")
    public ApiResponse<RefundResponse> sync(
            @Parameter(description = "Original payment ID, not refund ID.", example = "42")
            @PathVariable @Positive Long id,
            @Parameter(description = "Optional recovered provider refund ID; cannot replace "
                    + "a saved ID.",
                    example = "re_example")
            @RequestParam(required = false) String providerRefundId) {
        return ApiResponse.success("Refund checked.", operations.syncRefund(id, providerRefundId));
    }

    @Operation(summary = "Get the refund for a payment",
            description = "Returns the existing refund with amount including tips, "
                    + "currency, provider ID, "
                    + "status, error, reason, and UTC timestamps. Does not contact the "
                    + "provider; use "
                    + "refund/sync to refresh the outcome.")
    @GetMapping("/refund")
    public ApiResponse<RefundResponse> get(
            @Parameter(description = "Original payment ID, not refund ID.", example = "42")
            @PathVariable @Positive Long id) {
        return ApiResponse.success("Refund retrieved.", operations.getRefund(id));
    }
}
