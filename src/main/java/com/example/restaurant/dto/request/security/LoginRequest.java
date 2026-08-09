package com.example.restaurant.dto.request.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Employee login credentials.")
public record LoginRequest(
        @Schema(description = "Employee email address.", example = "olena.kovalenko@example.com")
        @NotBlank
        @Email
        String email,
        @Schema(description = "Employee password.", example = "Str0ngPass!2026",
                format = "password")
        @NotBlank
        String password
){}
