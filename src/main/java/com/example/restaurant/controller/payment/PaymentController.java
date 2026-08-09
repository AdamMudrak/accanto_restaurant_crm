package com.example.restaurant.controller.payment;

import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.INTERNAL_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.NOT_FOUND_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.PAYMENT_CONFLICT_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.PAYMENT_RESPONSE;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.PAYMENT_VALIDATION_ERROR;

import com.example.restaurant.dto.request.payment.PaymentRequest;
import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.dto.response.list.payment.PaymentListResponse;
import com.example.restaurant.dto.response.payment.PaymentResponse;
import com.example.restaurant.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Guest payment creation and employee payment lookup.")
public class PaymentController {
    private final PaymentService paymentService;

    @Operation(
            summary = "List payments",
            description = "Returns paginated payments for CRM.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @Parameters({
            @Parameter(name = "page", description = "Zero-based page index.", example = "0"),
            @Parameter(name = "size", description = "Payments per page.", example = "20"),
            @Parameter(
                    name = "sort",
                    description = "Payment sort in property,direction format.",
                    example = "createdAt,desc")
    })
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<
            ApiResponse<
                    ApiResponse.PageResponse<
                            PaymentResponse>>> getPayments(
            @Parameter(hidden = true) Pageable pageable) {
        PaymentListResponse paymentListResponse = paymentService.getPayments(pageable);

        return new ResponseEntity<>(
                ApiResponse.pageSuccess(
                        "Successfully retrieved payments.",
                        paymentListResponse.items(),
                        paymentListResponse.pageNumber(),
                        paymentListResponse.pageSize(),
                        paymentListResponse.totalElements()),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Get payment",
            description = "Returns one payment for CRM.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable @Positive Long id) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully retrieved payment.",
                        paymentService.getPaymentById(id)),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Create payment",
            description = "Creates a payment for selected order items from guest cookies. "
                    + "The provider is selected from restaurant settings.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Payment created.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = PAYMENT_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = PAYMENT_VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Order, guest, restaurant, or order item was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Selected item is not confirmed or served, is already paid, "
                            + "or belongs to another payment item set.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = PAYMENT_CONFLICT_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server or provider error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> pay(
            @Valid @RequestBody PaymentRequest paymentRequest,
            HttpServletRequest request) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully obtained payment.",
                        paymentService.pay(paymentRequest, request)),
                HttpStatus.OK);
    }

}
