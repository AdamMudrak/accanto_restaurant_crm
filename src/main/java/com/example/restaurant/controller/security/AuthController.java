package com.example.restaurant.controller.security;

import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.BAD_REQUEST_ERROR;
import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.CONFLICT_ERROR;
import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.EMPLOYEE_CONFIRMED;
import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.EMPLOYEE_CREATED;
import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.FORBIDDEN_ERROR;
import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.GONE_ERROR;
import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.INTERNAL_ERROR;
import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.INVALID_TOKEN_ERROR;
import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.LOGIN_SUCCESS;
import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.LOGOUT_SUCCESS;
import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.NOT_FOUND_ERROR;
import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.PASSWORD_CHANGED;
import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.PASSWORD_SET;
import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.RESET_LINK_SENT;
import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.UNAUTHORIZED_ERROR;
import static com.example.restaurant.controller.openapi.AuthOpenApiExamples.VALIDATION_ERROR;

import com.example.restaurant.dto.request.security.ChangePasswordRequest;
import com.example.restaurant.dto.request.security.FinalizeEmployeeActionRequest;
import com.example.restaurant.dto.request.security.LoginRequest;
import com.example.restaurant.dto.request.security.RegisterEmployeeRequest;
import com.example.restaurant.dto.request.security.TriggerEmailActionRequest;
import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.dto.response.user.EmployeeResponse;
import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.service.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@Tag(
        name = "Authentication",
        description = "Employee registration, login, logout, and password flows.")
public class AuthController {
    private final AuthService authService;

    @Operation(
            summary = "Register employee",
            description = "Creates an employee account and sends an email confirmation link. "
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
                    responseCode = "201",
                    description = "Employee created and confirmation email sent.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = EMPLOYEE_CREATED))),
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
                    description = "Authenticated user does not have admin privileges.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
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
    @PostMapping(
            value = "/register-employee",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> registerEmployee(
            @Parameter(description = "Employee registration data.", required = true)
            @RequestPart(value = "employeeRequest")
            @Valid RegisterEmployeeRequest employeeRequest,
            @Parameter(description = "Optional jpg, jpeg, or png employee profile picture.")
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {
        EmployeeResponse employeeResponse =
                authService.registerEmployee(employeeRequest, profilePicture);

        return new ResponseEntity<>(ApiResponse.success(
                "Employee created and saved. Account confirmation email sent.",
                employeeResponse),
                HttpStatus.CREATED);
    }

    @Operation(
            summary = "Resend registration finalization email",
            description = "Sends a new account confirmation link to an unconfirmed employee.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Registration confirmation link sent."),
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
                    description = "Employee was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Employee email is already confirmed.",
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
    @PostMapping("/trigger-finalize-employee-registration")
    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> triggerFinalizeRegistration(
            @Valid @RequestBody TriggerEmailActionRequest request) {
        authService.triggerFinalizeRegistration(request);

        return new ResponseEntity<>(ApiResponse.emptySuccess(
                "Registration confirmation link has been sent successfully."),
                HttpStatus.NO_CONTENT);
    }

    @Operation(
            summary = "Finalize employee registration",
            description = "Confirms employee email and sets the initial password "
                    + "from an email token.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Email confirmed and password set.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = EMPLOYEE_CONFIRMED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
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
                    responseCode = "401",
                    description = "Token is not acceptable.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INVALID_TOKEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Email is already confirmed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = CONFLICT_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "410",
                    description = "Token is expired.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = GONE_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PostMapping("/finalize-employee-registration")
    public ResponseEntity<ApiResponse<EmployeeResponse>> finalizeRegistration(
            @Valid @RequestBody FinalizeEmployeeActionRequest request) {
        EmployeeResponse employeeResponse = authService.finalizeRegistration(request);

        return new ResponseEntity<>(ApiResponse.success(
                "Email confirmed, password set successfully.",
                employeeResponse),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Login",
            description = "Authenticates an employee and writes accessToken "
                    + "and refreshToken cookies.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login completed. JWT cookies are set in response headers.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = LOGIN_SUCCESS))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid login or password.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = UNAUTHORIZED_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Employee is deleted or email is not confirmed.",
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
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletResponse response) {
        authService.login(loginRequest, response);

        return new ResponseEntity<>(
                ApiResponse.emptySuccess("Login performed successfully."),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Logout",
            description = "Clears accessToken and refreshToken cookies.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Logout completed and JWT cookies cleared.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = LOGOUT_SUCCESS))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have a supported employee role.",
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
    @PostMapping("/logout")
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        authService.logout(response);

        return new ResponseEntity<>(ApiResponse.emptySuccess(
                "Logout performed successfully."),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Start password reset",
            description = "Sends a password reset link to an active employee email.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Password reset link sent.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = RESET_LINK_SENT))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Employee is deleted or email is not confirmed.",
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
    @PostMapping("/start-password-reset")
    public ResponseEntity<ApiResponse<Void>> startPasswordReset(
            @Valid @RequestBody TriggerEmailActionRequest request) {
        authService.startPasswordReset(request);

        return new ResponseEntity<>(
                ApiResponse.emptySuccess(
                        "Reset link has been sent successfully."),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Finalize password reset",
            description = "Validates an email token and sets a new employee password.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Password reset completed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = PASSWORD_SET))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
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
                    responseCode = "401",
                    description = "Token is not acceptable.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INVALID_TOKEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Employee is deleted.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "410",
                    description = "Token is expired.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = GONE_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PostMapping("/finalize-password-reset")
    public ResponseEntity<ApiResponse<Void>> finalizePasswordReset(
            @Valid @RequestBody FinalizeEmployeeActionRequest request) {
        authService.finalizePasswordReset(request);

        return new ResponseEntity<>(ApiResponse.emptySuccess(
                "Password set successfully."),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Change current password",
            description = "Changes the password for the authenticated employee.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Password changed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = PASSWORD_CHANGED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Current password does not match.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = UNAUTHORIZED_ERROR))),
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
    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal Employee employee,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(employee.getId(), request);

        return new ResponseEntity<>(ApiResponse.emptySuccess("New password set successfully."),
                HttpStatus.OK);
    }
}
