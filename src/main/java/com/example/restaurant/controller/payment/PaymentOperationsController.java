package com.example.restaurant.controller.payment;

import com.example.restaurant.dto.request.payment.LinkInvoiceRequest;
import com.example.restaurant.dto.request.payment.RefundRequest;
import com.example.restaurant.dto.request.payment.ResolvePaymentRequest;
import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.dto.response.payment.PaymentResponse;
import com.example.restaurant.dto.response.payment.RefundResponse;
import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.service.payment.PaymentOperationsService;
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
public class PaymentOperationsController {
    private final PaymentOperationsService operations;

    @PostMapping("/invoice")
    public ApiResponse<PaymentResponse> link(@PathVariable @Positive Long id,
            @Valid @RequestBody LinkInvoiceRequest request,
            @AuthenticationPrincipal Employee actor) {
        return ApiResponse.success("Provider invoice verified.", operations.link(id, request,
                actor));
    }

    @PostMapping("/resolution")
    public ApiResponse<PaymentResponse> resolve(@PathVariable @Positive Long id,
            @Valid @RequestBody ResolvePaymentRequest request,
            @AuthenticationPrincipal Employee actor) {
        return ApiResponse.success("Payment issue resolved.", operations.resolve(id, request,
                actor));
    }

    @PostMapping("/refund")
    public ApiResponse<RefundResponse> refund(@PathVariable @Positive Long id,
            @Valid @RequestBody RefundRequest request, @AuthenticationPrincipal Employee actor) {
        return ApiResponse.success("Refund recorded.", operations.refund(id, request, actor));
    }

    @PostMapping("/refund/retry")
    public ApiResponse<RefundResponse> retry(@PathVariable @Positive Long id) {
        return ApiResponse.success("Refund retry recorded.", operations.retryRefund(id));
    }

    @PostMapping("/refund/sync")
    public ApiResponse<RefundResponse> sync(@PathVariable @Positive Long id,
            @RequestParam(required = false) String providerRefundId) {
        return ApiResponse.success("Refund checked.", operations.syncRefund(id, providerRefundId));
    }

    @GetMapping("/refund")
    public ApiResponse<RefundResponse> get(@PathVariable @Positive Long id) {
        return ApiResponse.success("Refund retrieved.", operations.getRefund(id));
    }
}
