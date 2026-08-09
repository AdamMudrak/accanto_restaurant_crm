package com.example.restaurant.controller.management;

import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.dto.response.analytics.CashlessTipPaymentResponse;
import com.example.restaurant.dto.response.analytics.EmployeeTipsResponse;
import com.example.restaurant.dto.response.analytics.MonthlyAnalyticsResponse;
import com.example.restaurant.dto.response.list.analytics.CashlessTipPaymentListResponse;
import com.example.restaurant.dto.response.list.order.OrderListResponse;
import com.example.restaurant.dto.response.order.OrderResponse;
import com.example.restaurant.entity.order.Order;
import com.example.restaurant.service.management.AdminAnalyticsService;
import com.example.restaurant.service.order.order.OrderEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
@Tag(
        name = "Admin analytics",
        description = "Admin endpoints for order history, cashless revenue, and cashless tips.")
public class AdminAnalyticsController {
    private static final int MIN_ANALYTICS_YEAR = 1970;
    private static final int MIN_MONTH = 1;
    private static final int MAX_MONTH = 12;

    private final OrderEmployeeService orderEmployeeService;
    private final AdminAnalyticsService adminAnalyticsService;

    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    @GetMapping("/orders")
    @Operation(
            summary = "Get admin order history",
            description = "Returns paged order history with optional status and date-time filters.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @Parameters({
            @Parameter(name = "page", description = "Zero-based page index.", example = "0"),
            @Parameter(name = "size", description = "Orders per page.", example = "20"),
            @Parameter(
                    name = "sort",
                    description = "Order sort in property,direction format.",
                    example = "createdAt,desc")
    })
    public ResponseEntity<
            ApiResponse<
                    ApiResponse.PageResponse<
                            OrderResponse>>> getOrderHistory(
            @Parameter(description = "Optional order status filter.", example = "CLOSED")
            @RequestParam(required = false) Order.OrderStatus status,
            @Parameter(description = "Optional inclusive start date-time filter.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Optional inclusive end date-time filter.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(hidden = true)
            Pageable pageable) {
        OrderListResponse orderListResponse =
                orderEmployeeService.getOrderHistory(status, from, to, pageable);

        return new ResponseEntity<>(
                ApiResponse.pageSuccess(
                        "Successfully retrieved admin order history.",
                        orderListResponse.orderResponses(),
                        orderListResponse.pageNumber(),
                        orderListResponse.pageSize(),
                        orderListResponse.totalElements()),
                HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    @GetMapping("/monthly-summary")
    @Operation(
            summary = "Get monthly cashless analytics summary",
            description = "Returns cashless revenue, cashless tips, and successful payment count.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    public ResponseEntity<ApiResponse<MonthlyAnalyticsResponse>> getMonthlySummary(
            @Parameter(description = "Analytics year.", example = "2026")
            @RequestParam @Min(MIN_ANALYTICS_YEAR) int year,
            @Parameter(description = "Analytics month number from 1 to 12.", example = "8")
            @RequestParam @Min(MIN_MONTH) @Max(MAX_MONTH) int month) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully retrieved monthly analytics.",
                        adminAnalyticsService.getMonthlySummary(year, month)),
                HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    @GetMapping("/tips/employees")
    @Operation(
            summary = "Get cashless tips grouped by employee",
            description = "Returns total, paid, and unpaid cashless tip amounts per employee.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    public ResponseEntity<ApiResponse<List<EmployeeTipsResponse>>> getEmployeeTips(
            @Parameter(description = "Analytics year.", example = "2026")
            @RequestParam @Min(MIN_ANALYTICS_YEAR) int year,
            @Parameter(description = "Analytics month number from 1 to 12.", example = "8")
            @RequestParam @Min(MIN_MONTH) @Max(MAX_MONTH) int month) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully retrieved employee tips.",
                        adminAnalyticsService.getEmployeeTips(year, month)),
                HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    @GetMapping("/tips/payments")
    @Operation(
            summary = "Get cashless tip payments",
            description = "Returns paged cashless payments with tips and payout state.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @Parameters({
            @Parameter(name = "page", description = "Zero-based page index.", example = "0"),
            @Parameter(name = "size", description = "Tip payments per page.", example = "20"),
            @Parameter(
                    name = "sort",
                    description = "Payment sort in property,direction format.",
                    example = "completedAt,desc")
    })
    public ResponseEntity<
            ApiResponse<
                    ApiResponse.PageResponse<
                            CashlessTipPaymentResponse>>> getTipPayments(
            @Parameter(description = "Analytics year.", example = "2026")
            @RequestParam @Min(MIN_ANALYTICS_YEAR) int year,
            @Parameter(description = "Analytics month number from 1 to 12.", example = "8")
            @RequestParam @Min(MIN_MONTH) @Max(MAX_MONTH) int month,
            @Parameter(description = "Optional payout status filter.", example = "false")
            @RequestParam(required = false) Boolean tipPaid,
            @Parameter(hidden = true)
            Pageable pageable) {
        CashlessTipPaymentListResponse response =
                adminAnalyticsService.getTipPayments(year, month, tipPaid, pageable);

        return new ResponseEntity<>(
                ApiResponse.pageSuccess(
                        "Successfully retrieved cashless tip payments.",
                        response.items(),
                        response.pageNumber(),
                        response.pageSize(),
                        response.totalElements()),
                HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    @PutMapping("/tips/payments/{paymentId}/paid")
    @Operation(
            summary = "Mark a cashless tip as paid",
            description = "Marks a cashless payment tip as paid out to the employee.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    public ResponseEntity<ApiResponse<CashlessTipPaymentResponse>> markTipPaid(
            @Parameter(description = "Payment id.", example = "1")
            @PathVariable @Positive Long paymentId) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully marked tip as paid.",
                        adminAnalyticsService.markTipPaid(paymentId)),
                HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    @PutMapping("/tips/payments/{paymentId}/unpaid")
    @Operation(
            summary = "Mark a cashless tip as unpaid",
            description = "Marks a cashless payment tip as not paid out to the employee.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    public ResponseEntity<ApiResponse<CashlessTipPaymentResponse>> markTipUnpaid(
            @Parameter(description = "Payment id.", example = "1")
            @PathVariable @Positive Long paymentId) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully marked tip as unpaid.",
                        adminAnalyticsService.markTipUnpaid(paymentId)),
                HttpStatus.OK);
    }
}
