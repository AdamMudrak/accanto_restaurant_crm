package com.example.restaurant.dto.request.user;

import com.example.restaurant.dto.annotations.phone.Phone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Payload used to update employee profile and admin-managed flags.")
public record UpdateEmployeeRequest(
        @Schema(description = "Employee first name.", example = "Olena")
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        String firstName,
        @Schema(description = "Employee last name.", example = "Kovalenko")
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        String lastName,
        @Schema(description = "Employee email address.", example = "olena.kovalenko@example.com")
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        @Email
        String email,
        @Schema(description = "Employee phone number.", example = "+380501112233")
        @Phone
        String phone,
        @Schema(description = "Whether the employee should have admin privileges.",
                example = "false")
        Boolean isAdmin,
        @Schema(description = "Whether the employee account should be soft-deleted.",
                example = "false")
        Boolean isDeleted
) {}
