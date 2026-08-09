package com.example.restaurant.dto.request.security;

import com.example.restaurant.dto.annotations.phone.Phone;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Payload used by an admin to create a new employee account.")
public record RegisterEmployeeRequest(
        @Schema(description = "Employee first name.", example = "Olena")
        @NotBlank
        String firstName,
        @Schema(description = "Employee last name.", example = "Kovalenko")
        @NotBlank
        String lastName,
        @Schema(description = "Employee email address.", example = "olena.kovalenko@example.com")
        @NotBlank
        @Email
        String email,
        @Schema(description = "Employee phone number.", example = "+380501112233")
        @NotBlank
        @Phone
        String phone,
        @Schema(description = "Whether the employee should receive admin privileges.",
                example = "false")
        boolean isAdmin
){}
