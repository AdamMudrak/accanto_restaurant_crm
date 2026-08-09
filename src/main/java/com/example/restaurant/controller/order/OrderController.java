package com.example.restaurant.controller.order;

import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.CONFLICT_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.CURRENT_GUEST_ORDER_RESPONSE;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.FORBIDDEN_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.INTERNAL_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.NOT_FOUND_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.ORDERS_RESPONSE;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.ORDER_CONFLICT_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.ORDER_ITEMS_BY_GUEST_RESPONSE;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.ORDER_RESPONSE;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.VALIDATION_ERROR;

import com.example.restaurant.dto.request.order.AddOrderItemEmployeeRequest;
import com.example.restaurant.dto.request.order.AddOrderItemRequest;
import com.example.restaurant.dto.request.order.CloseOrderManuallyRequest;
import com.example.restaurant.dto.request.order.UpdateOrderItemEmployeeRequest;
import com.example.restaurant.dto.request.order.UpdateOrderItemRequest;
import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.dto.response.list.order.OrderListResponse;
import com.example.restaurant.dto.response.list.payment.PaymentListResponse;
import com.example.restaurant.dto.response.order.CurrentGuestOrderResponse;
import com.example.restaurant.dto.response.order.OrderItemsByGuestResponse;
import com.example.restaurant.dto.response.order.OrderResponse;
import com.example.restaurant.dto.response.payment.PaymentResponse;
import com.example.restaurant.entity.order.Order;
import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.service.order.order.OrderEmployeeService;
import com.example.restaurant.service.order.order.OrderGuestService;
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
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Guest and employee order item operations.")
public class OrderController {
    private final OrderGuestService orderGuestService;
    private final OrderEmployeeService orderEmployeeService;
    private final PaymentService paymentService;

    @Operation(
            summary = "List opened orders",
            description = "Returns opened orders for employees.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @Parameters({
            @Parameter(name = "page", description = "Zero-based page index.", example = "0"),
            @Parameter(name = "size", description = "Orders per page.", example = "20"),
            @Parameter(
                    name = "sort",
                    description = "Order sort in property,direction format.",
                    example = "createdAt,desc")
    })
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Orders retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = ORDERS_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have employee access.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<
            ApiResponse<
                    ApiResponse.PageResponse<
                            OrderResponse>>> getAllOpenOrders(
                                    @Parameter(hidden = true) Pageable pageable) {
        OrderListResponse orderListResponse = orderEmployeeService.getAllOpenOrders(pageable);

        return new ResponseEntity<>(
                ApiResponse.pageSuccess(
                        "Successfully retrieved orders.",
                        orderListResponse.orderResponses(),
                        orderListResponse.pageNumber(),
                        orderListResponse.pageSize(),
                        orderListResponse.totalElements()),
                HttpStatus.OK);
    }

    @Operation(
            summary = "List order history",
            description = "Returns paged order history with optional status and date-time filters.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @Parameters({
            @Parameter(name = "status", description = "Optional order status filter.",
                    example = "CLOSED"),
            @Parameter(name = "from", description = "Optional inclusive start date-time filter."),
            @Parameter(name = "to", description = "Optional inclusive end date-time filter."),
            @Parameter(name = "page", description = "Zero-based page index.", example = "0"),
            @Parameter(name = "size", description = "Orders per page.", example = "20"),
            @Parameter(
                    name = "sort",
                    description = "Order sort in property,direction format.",
                    example = "createdAt,desc")
    })
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Order history retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = ORDERS_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have employee access.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    @GetMapping("/history")
    public ResponseEntity<
            ApiResponse<
                    ApiResponse.PageResponse<
                            OrderResponse>>> getOrderHistory(
            @RequestParam(required = false) Order.OrderStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(hidden = true) Pageable pageable) {
        OrderListResponse orderListResponse =
                orderEmployeeService.getOrderHistory(status, from, to, pageable);

        return new ResponseEntity<>(
                ApiResponse.pageSuccess(
                        "Successfully retrieved order history.",
                        orderListResponse.orderResponses(),
                        orderListResponse.pageNumber(),
                        orderListResponse.pageSize(),
                        orderListResponse.totalElements()),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Get current guest order",
            description = "Returns the current guest order using guest, table, and order cookies.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Current guest order retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = CURRENT_GUEST_ORDER_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Guest, table, or order from cookies was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Guest or order state does not allow this operation.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = CONFLICT_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @GetMapping("/guest/current")
    public ResponseEntity<ApiResponse<CurrentGuestOrderResponse>> getCurrentGuestOrder(
            HttpServletRequest httpRequest) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully retrieved current order.",
                        orderGuestService.getCurrentOrder(httpRequest)),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Get opened order",
            description = "Returns one opened order with items grouped by guest.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Order retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = ORDER_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have employee access.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Opened order was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable @Positive Long id) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully retrieved order.",
                        orderEmployeeService.getOrderById(id)),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Guest adds order item",
            description = "Adds a menu item to the opened order from guest cookies.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Order item added.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = CURRENT_GUEST_ORDER_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Order, guest, or menu item was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Guest or item state does not allow this operation.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = CONFLICT_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PostMapping("/guest/items")
    public ResponseEntity<ApiResponse<CurrentGuestOrderResponse>> addGuestOrderItem(
            @Valid @RequestBody AddOrderItemRequest request,
            HttpServletRequest httpRequest) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully added order item.",
                        orderGuestService.addOrderItem(httpRequest, request)),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Guest updates order item",
            description = "Updates or cancels guest's own not-yet-confirmed order item.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Order item updated.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = CURRENT_GUEST_ORDER_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Order, guest, or order item was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Guest or item state does not allow this operation.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = CONFLICT_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PutMapping("/guest/items/{id}")
    public ResponseEntity<ApiResponse<CurrentGuestOrderResponse>> updateGuestOrderItem(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateOrderItemRequest request,
            HttpServletRequest httpRequest) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully updated order item.",
                        orderGuestService.updateOrderItem(id, httpRequest, request)),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Employee adds order item",
            description = "Adds item to an opened order. If guestId is absent, creates a guest.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Order item added.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = ORDER_ITEMS_BY_GUEST_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have employee access.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Order, guest, or menu item was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Guest state does not allow this operation.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = CONFLICT_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    @PostMapping("/{id}/items")
    public ResponseEntity<ApiResponse<List<OrderItemsByGuestResponse>>> addEmployeeOrderItem(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AddOrderItemEmployeeRequest request) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully added order item.",
                        orderEmployeeService.addOrderItem(id, request)),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Employee updates order item",
            description = "Updates quantity, employee note, or employee-controlled item status.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Order item updated.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = ORDER_ITEMS_BY_GUEST_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have employee access.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Order item was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Item state does not allow this operation.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = CONFLICT_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<List<OrderItemsByGuestResponse>>> updateEmployeeOrderItem(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateOrderItemEmployeeRequest request,
            @AuthenticationPrincipal Employee employee) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully updated order item.",
                        orderEmployeeService.updateOrderItem(id, request, employee)),
                HttpStatus.OK);
    }

    @Operation(
            summary = "List order payments",
            description = "Returns paged cashless payments for an order.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @Parameters({
            @Parameter(name = "page", description = "Zero-based page index.", example = "0"),
            @Parameter(name = "size", description = "Payments per page.", example = "20"),
            @Parameter(
                    name = "sort",
                    description = "Payment sort in property,direction format.",
                    example = "createdAt,desc")
    })
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Order payments retrieved.",
                    content = @Content(
                            mediaType = "application/json")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have employee access.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Order was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    @GetMapping("/{id}/payments")
    public ResponseEntity<
            ApiResponse<
                    ApiResponse.PageResponse<
                            PaymentResponse>>> getOrderPayments(
            @PathVariable @Positive Long id,
            @Parameter(hidden = true) Pageable pageable) {
        PaymentListResponse paymentListResponse =
                paymentService.getPaymentsByOrderId(id, pageable);

        return new ResponseEntity<>(
                ApiResponse.pageSuccess(
                        "Successfully retrieved order payments.",
                        paymentListResponse.items(),
                        paymentListResponse.pageNumber(),
                        paymentListResponse.pageSize(),
                        paymentListResponse.totalElements()),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Close order",
            description = "Closes opened order when all items are paid or canceled.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Order closed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = ORDER_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have employee access.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Opened order was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Order still has unpaid items.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = ORDER_CONFLICT_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponse<OrderResponse>> closeOrder(
            @PathVariable @Positive Long id) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully closed order.",
                        orderEmployeeService.closeOrder(id)),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Manually close order",
            description = "Closes an order after marking unpaid items as paid cash or canceled.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Order manually closed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = ORDER_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have employee access.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Opened order was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Order state does not allow manual closing.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = ORDER_CONFLICT_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    @PutMapping("/{id}/close-manually")
    public ResponseEntity<ApiResponse<OrderResponse>> closeOrderManually(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CloseOrderManuallyRequest request) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully closed order manually.",
                        orderEmployeeService.closeOrderManually(id, request)),
                HttpStatus.OK);
    }
}
