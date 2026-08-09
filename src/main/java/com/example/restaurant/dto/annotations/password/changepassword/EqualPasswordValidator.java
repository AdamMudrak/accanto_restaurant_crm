package com.example.restaurant.dto.annotations.password.changepassword;

import com.example.restaurant.dto.request.security.ChangePasswordRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EqualPasswordValidator
        implements ConstraintValidator<EqualPassword, ChangePasswordRequest> {

    @Override
    public boolean isValid(ChangePasswordRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        if (request.getNewPassword() == null || request.getConfirmPassword() == null) {
            return true;
        }

        return request.getNewPassword().equals(request.getConfirmPassword());
    }
}
