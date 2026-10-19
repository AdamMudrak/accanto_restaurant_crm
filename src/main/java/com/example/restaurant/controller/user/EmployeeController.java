package com.example.restaurant.controller.user;

import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.BAD_REQUEST_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.CONFLICT_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.EMPLOYEES_PAGE;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.EMPLOYEE_RESPONSE;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.FORBIDDEN_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.INTERNAL_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.NOT_FOUND_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.VALIDATION_ERROR;

import com.example.restaurant.dto.request.user.FinalizeEmailChangeRequest;
import com.example.restaurant.dto.request.user.UpdateEmployeeRequest;
import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.dto.response.list.user.EmployeeListResponse;
import com.example.restaurant.dto.response.user.EmployeeResponse;
import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.service.user.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/employees")
@Tag(
        name = "Employees",
        description = "Employee profile, list, update, and email-change endpoints.")
public class EmployeeController {
    private final EmployeeService employeeService;

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    @Operation(summary = "Soft-delete employee",
            description = "Preserves history. Rejects the last active administrator and employees "
                    + "with open orders. Repeated deletion succeeds.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable @Positive Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.emptySuccess("Employee deleted."));
    }

    @Operation(
            summary = "Get employee",
            description = "Returns employee details by identifier. Admin access is required.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Employee retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = EMPLOYEE_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Path variable validation failed.",
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
                    description = "Employee was not found.",
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
    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeById(
            @PathVariable @Positive Long id) {
        EmployeeResponse employeeResponse = employeeService.getEmployeeById(id);

        return new ResponseEntity<>(
                ApiResponse.success("Employee with id " + id + " found.",
                        employeeResponse),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Get current employee",
            description = "Returns the authenticated employee profile.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Employee retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = EMPLOYEE_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have a supported employee role.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Employee was not found.",
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
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getMe(
            @AuthenticationPrincipal Employee employee) {
        EmployeeResponse employeeResponse = employeeService.getEmployeeById(employee.getId());

        return new ResponseEntity<>(
                ApiResponse.success("Employee with id " + employee.getId() + " found.",
                        employeeResponse),
                HttpStatus.OK);
    }

    @Operation(
            summary = "List employees",
            description = "Returns a paginated employee list. Admin access is required.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @Parameters({
            @Parameter(name = "page", description = "Zero-based page index.", example = "0"),
            @Parameter(name = "size", description = "Employees per page.", example = "20"),
            @Parameter(
                    name = "sort",
                    description = "Employee sort in property,direction format.",
                    example = "lastName,asc")
    })
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Employees retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = EMPLOYEES_PAGE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have admin privileges.",
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
    @GetMapping
    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    public ResponseEntity<
            ApiResponse<
                    ApiResponse.PageResponse<
                            EmployeeResponse>>> getEmployees(
                                    @Parameter(hidden = true) Pageable pageable) {
        EmployeeListResponse employeeListResponse = employeeService.getEmployees(pageable);

        return new ResponseEntity<>(
                ApiResponse.pageSuccess(
                        "Employees retrieved successfully.",
                        employeeListResponse.employees(),
                        employeeListResponse.pageNumber(),
                        employeeListResponse.pageSize(),
                        employeeListResponse.totalElements()),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Update employee",
            description = "Updates an employee by identifier. Admin access is required. "
                    + "The request is sent as multipart parts: employeeRequest JSON "
                    + "and optional profilePicture.",
            security = @SecurityRequirement(name = "accessTokenCookie"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            encoding = @Encoding(
                                    name = "employeeRequest",
                                    contentType = MediaType.APPLICATION_JSON_VALUE))))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Employee updated.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = EMPLOYEE_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed or profile picture is invalid.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Validation error",
                                            value = VALIDATION_ERROR),
                                    @ExampleObject(
                                            name = "Invalid image",
                                            value = BAD_REQUEST_ERROR)
                            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have admin privileges "
                            + "or attempts a forbidden self-modification.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Employee was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Email is already used.",
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
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployeeById(
            @AuthenticationPrincipal Employee employee,
            @PathVariable @Positive Long id,
            @Parameter(description = "Optional employee update data.")
            @RequestPart(value = "employeeRequest", required = false)
            @Valid UpdateEmployeeRequest employeeRequest,
            @Parameter(description = "Optional jpg, jpeg, or png employee profile picture.")
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {
        EmployeeResponse employeeResponse =
                employeeService.updateEmployeeById(
                        employee.getId(), id, employeeRequest, profilePicture);

        return new ResponseEntity<>(ApiResponse.success(
                "Updated employee successfully.",
                employeeResponse), HttpStatus.OK);
    }

    @Operation(
            summary = "Update current employee",
            description = "Updates the authenticated employee profile. The request is sent "
                    + "as multipart parts: employeeRequest JSON and optional profilePicture.",
            security = @SecurityRequirement(name = "accessTokenCookie"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            encoding = @Encoding(
                                    name = "employeeRequest",
                                    contentType = MediaType.APPLICATION_JSON_VALUE))))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Employee updated.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = EMPLOYEE_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed or profile picture is invalid.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Validation error",
                                            value = VALIDATION_ERROR),
                                    @ExampleObject(
                                            name = "Invalid image",
                                            value = BAD_REQUEST_ERROR)
                            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have a supported employee role.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Employee was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Email is already used.",
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
    @PutMapping(
            value = "/me",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateMe(
            @AuthenticationPrincipal Employee employee,
            @Parameter(description = "Optional employee update data.")
            @RequestPart(value = "employeeRequest", required = false)
            @Valid UpdateEmployeeRequest employeeRequest,
            @Parameter(description = "Optional jpg, jpeg, or png employee profile picture.")
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {
        EmployeeResponse employeeResponse =
                employeeService.updateEmployeeById(
                        employee.getId(), employee.getId(), employeeRequest, profilePicture);

        return new ResponseEntity<>(ApiResponse.success(
                "Updated employee successfully.",
                employeeResponse), HttpStatus.OK);
    }

    @Operation(
            summary = "Finalize email change",
            description = "Confirms an employee email change from an email token.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Email change finalized.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = EMPLOYEE_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Token is not acceptable.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Employee or token was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Email is already used or already confirmed.",
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
    @PostMapping("/finalize-email-change")
    public ResponseEntity<ApiResponse<EmployeeResponse>> finalizeEmailChange(
            @Valid @RequestBody FinalizeEmailChangeRequest request, HttpServletResponse response) {
        EmployeeResponse employeeResponse = employeeService.finalizeEmailChange(request, response);

        return new ResponseEntity<>(ApiResponse.success(
                "Changed email confirmed.",
                employeeResponse),
                HttpStatus.OK);
    }
}
