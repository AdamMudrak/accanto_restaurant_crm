package com.example.restaurant.dto.request.security;

import com.example.restaurant.dto.annotations.password.Password;
import com.example.restaurant.dto.annotations.password.changepassword.EqualPassword;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@EqualPassword
@Getter
@Setter
@Schema(description = "Payload used by an authenticated employee to change their password.")
public class ChangePasswordRequest {
    @Schema(description = "Current password.", example = "OldStr0ngPass!2026",
            format = "password")
    @NotBlank
    @Password
    private String currentPassword;
    @Schema(description = "New password.", example = "NewStr0ngPass!2026", format = "password")
    @NotBlank
    @Password
    private String newPassword;
    @Schema(description = "New password confirmation.", example = "NewStr0ngPass!2026",
            format = "password")
    @NotBlank
    @Password
    private String confirmPassword;
}
