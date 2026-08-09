package com.example.restaurant.controller.test;

import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.EMPTY_SUCCESS;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.FORBIDDEN_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.INTERNAL_ERROR;

import com.example.restaurant.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Tag(
        name = "Test",
        description = "Authentication and authorization test endpoints.")
public class TestController {
    @Operation(
            summary = "Test admin authentication",
            description = "Returns success only for authenticated admin employees.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Admin authorization succeeded.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = EMPTY_SUCCESS))),
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
    @GetMapping("/admin-auth")
    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> testAdmin() {
        return new ResponseEntity<>(ApiResponse.emptySuccess("You are admin"), HttpStatus.OK);
    }

    @Operation(
            summary = "Test employee authentication",
            description = "Returns success for authenticated admin or regular employees.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Employee authorization succeeded.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = EMPTY_SUCCESS))),
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
    @GetMapping("/any-auth")
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> testAny() {
        return new ResponseEntity<>(
                ApiResponse.emptySuccess("You are admin or employee"), HttpStatus.OK);
    }

    @Operation(
            summary = "Test public access",
            description = "Returns success without authentication.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Public endpoint reached.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = EMPTY_SUCCESS))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @GetMapping("/no-auth")
    public ResponseEntity<ApiResponse<Void>> testNoAuth() {
        return new ResponseEntity<>(
                ApiResponse.emptySuccess("No auth test success"), HttpStatus.OK);
    }
}
