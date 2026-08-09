package com.example.restaurant.dto.response.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Employee details returned by the API.")
public record EmployeeResponse(
        @Schema(description = "Employee identifier.", example = "12")
        Long id,
        @Schema(description = "Employee first name.", example = "Olena")
        String firstName,
        @Schema(description = "Employee last name.", example = "Kovalenko")
        String lastName,
        @Schema(description = "Employee email address.", example = "olena.kovalenko@example.com")
        String email,
        @Schema(description = "Whether the employee confirmed their email.", example = "true")
        boolean emailConfirmed,
        @Schema(description = "Profile picture image identifier.", example = "7", nullable = true)
        Long profilePictureId,
        @Schema(description = "Employee phone number.", example = "+380501112233")
        String phone,
        @Schema(description = "Whether the employee account is soft-deleted.", example = "false")
        boolean isDeleted
){}
