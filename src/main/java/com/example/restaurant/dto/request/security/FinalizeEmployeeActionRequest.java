package com.example.restaurant.dto.request.security;

import com.example.restaurant.dto.annotations.password.Password;
import com.example.restaurant.dto.annotations.password.setpassword.EqualPassword;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@EqualPassword
@Getter
@Setter
@Schema(description = "Payload used to finalize registration or password reset.")
public class FinalizeEmployeeActionRequest {
    @Schema(description = "Employee identifier from the email action link.", example = "12")
    @NotNull
    @Positive
    private Long employeeId;
    @Schema(description = "UUID token from the email action link.",
            example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank
    @UUID
    private String uuid;
    @Schema(description = "New password.", example = "Str0ngPass!2026", format = "password")
    @NotBlank
    @Password
    private String password;
    @Schema(description = "New password confirmation.", example = "Str0ngPass!2026",
            format = "password")
    @NotBlank
    @Password
    private String confirmPassword;
}
