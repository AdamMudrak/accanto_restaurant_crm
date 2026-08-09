package com.example.restaurant.dto.request.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@Schema(description = "Payload used to confirm an employee email address change.")
public class FinalizeEmailChangeRequest {
    @Schema(description = "Employee identifier from the email action link.", example = "12")
    @NotNull
    @Positive
    private Long employeeId;
    @Schema(description = "UUID token from the email action link.",
            example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank
    @UUID
    private String uuid;
}
