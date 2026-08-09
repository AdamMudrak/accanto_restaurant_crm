package com.example.restaurant.controller.management;

import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.CONFLICT_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.FORBIDDEN_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.INTERNAL_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.NOT_FOUND_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.VALIDATION_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.WORKING_DAYS_RESPONSE;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.WORKING_DAY_RESPONSE;

import com.example.restaurant.dto.request.management.UpdateWorkingDayRequest;
import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.dto.response.list.management.WorkingDayListResponse;
import com.example.restaurant.dto.response.management.WorkingDayResponse;
import com.example.restaurant.service.management.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
@Tag(
        name = "Schedule",
        description = "Restaurant working day schedule.")
public class ScheduleController {
    private final ScheduleService scheduleService;

    @Operation(
            summary = "Get working day",
            description = "Returns one working day by identifier.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Working day retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = WORKING_DAY_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Working day was not found.",
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
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkingDayResponse>> getWorkingDayById(
            @PathVariable Long id) {
        WorkingDayResponse workingDayResponse = scheduleService.getWorkingDayById(id);

        return new ResponseEntity<>(ApiResponse.success(
                "Retrieved working day successfully.", workingDayResponse), HttpStatus.OK);
    }

    @Operation(
            summary = "Get working days",
            description = "Returns all restaurant working days ordered by id.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Working days retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = WORKING_DAYS_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @GetMapping
    public ResponseEntity<
            ApiResponse<
                    ApiResponse.PageResponse<
                            WorkingDayResponse>>> getWorkingDays() {
        WorkingDayListResponse workingDayListResponse = scheduleService.getWorkingDays();

        return new ResponseEntity<>(ApiResponse.pageSuccess(
                "Retrieved working days successfully.",
                workingDayListResponse.workingDays(),
                workingDayListResponse.pageNumber(),
                workingDayListResponse.pageSize(),
                workingDayListResponse.totalElements()
                ), HttpStatus.OK);
    }

    @Operation(
            summary = "Update working day",
            description = "Updates active flag and present time fields for one working day.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Working day updated.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = WORKING_DAY_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have admin privileges.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Working day was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Time fields conflict with current working day values.",
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
    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkingDayResponse>> updateWorkingDayById(
            @PathVariable Long id, @Valid @RequestBody UpdateWorkingDayRequest request) {
        WorkingDayResponse workingDayResponse = scheduleService.updateWorkingDayById(id, request);

        return new ResponseEntity<>(ApiResponse.success(
                "Updated working day successfully.", workingDayResponse), HttpStatus.OK);
    }
}
