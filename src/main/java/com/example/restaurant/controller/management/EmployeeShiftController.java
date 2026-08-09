package com.example.restaurant.controller.management;

import com.example.restaurant.dto.request.management.CreateEmployeeShiftRequest;
import com.example.restaurant.dto.request.management.UpdateEmployeeShiftRequest;
import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.dto.response.list.management.EmployeeShiftListResponse;
import com.example.restaurant.dto.response.management.EmployeeShiftResponse;
import com.example.restaurant.service.management.EmployeeShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/admin/employee-shifts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
@Tag(
        name = "Employee shifts",
        description = "Admin endpoints for employee schedule management.")
public class EmployeeShiftController {
    private final EmployeeShiftService employeeShiftService;

    @GetMapping
    @Operation(
            summary = "Get employee shifts",
            description = "Returns paged employee shifts with optional employee and date filters.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @Parameters({
            @Parameter(name = "page", description = "Zero-based page index.", example = "0"),
            @Parameter(name = "size", description = "Employee shifts per page.", example = "20"),
            @Parameter(
                    name = "sort",
                    description = "Employee shift sort in property,direction format.",
                    example = "startsAt,asc")
    })
    public ResponseEntity<
            ApiResponse<
                    ApiResponse.PageResponse<
                            EmployeeShiftResponse>>> getEmployeeShifts(
            @Parameter(description = "Optional employee id filter.", example = "2")
            @RequestParam(required = false) @Positive Long employeeId,
            @Parameter(description = "Optional inclusive start work date.", example = "2026-08-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Optional inclusive end work date.", example = "2026-08-31")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(hidden = true)
            Pageable pageable) {
        EmployeeShiftListResponse response =
                employeeShiftService.getEmployeeShifts(employeeId, from, to, pageable);

        return new ResponseEntity<>(
                ApiResponse.pageSuccess(
                        "Successfully retrieved employee shifts.",
                        response.items(),
                        response.pageNumber(),
                        response.pageSize(),
                        response.totalElements()),
                HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get employee shift by id",
            description = "Returns a single employee shift by id.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    public ResponseEntity<ApiResponse<EmployeeShiftResponse>> getEmployeeShiftById(
            @Parameter(description = "Employee shift id.", example = "1")
            @PathVariable @Positive Long id) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully retrieved employee shift.",
                        employeeShiftService.getEmployeeShiftById(id)),
                HttpStatus.OK);
    }

    @PostMapping
    @Operation(
            summary = "Create employee shift",
            description = "Creates a new employee shift.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    public ResponseEntity<ApiResponse<EmployeeShiftResponse>> createEmployeeShift(
            @Parameter(description = "Employee shift creation payload.")
            @Valid @RequestBody CreateEmployeeShiftRequest request) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully created employee shift.",
                        employeeShiftService.createEmployeeShift(request)),
                HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update employee shift",
            description = "Updates an existing employee shift by id.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    public ResponseEntity<ApiResponse<EmployeeShiftResponse>> updateEmployeeShift(
            @Parameter(description = "Employee shift id.", example = "1")
            @PathVariable @Positive Long id,
            @Parameter(description = "Employee shift update payload.")
            @Valid @RequestBody UpdateEmployeeShiftRequest request) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully updated employee shift.",
                        employeeShiftService.updateEmployeeShiftById(id, request)),
                HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete employee shift",
            description = "Deletes an employee shift by id.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    public ResponseEntity<ApiResponse<Void>> deleteEmployeeShift(
            @Parameter(description = "Employee shift id.", example = "1")
            @PathVariable @Positive Long id) {
        employeeShiftService.deleteEmployeeShiftById(id);

        return new ResponseEntity<>(
                ApiResponse.emptySuccess("Successfully deleted employee shift."),
                HttpStatus.OK);
    }
}
