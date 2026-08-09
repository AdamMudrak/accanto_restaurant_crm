package com.example.restaurant.dto.request.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload used to send an email action link.")
public record TriggerEmailActionRequest(
        @Schema(description = "Employee email address.", example = "olena.kovalenko@example.com")
        @NotBlank @Email
        String email
) {}
