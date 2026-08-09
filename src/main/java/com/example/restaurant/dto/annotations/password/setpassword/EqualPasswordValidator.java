package com.example.restaurant.dto.annotations.password.setpassword;

import com.example.restaurant.dto.request.security.FinalizeEmployeeActionRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EqualPasswordValidator
        implements ConstraintValidator<EqualPassword, FinalizeEmployeeActionRequest> {

    @Override
    public boolean isValid(FinalizeEmployeeActionRequest request,
                           ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        if (request.getPassword() == null || request.getConfirmPassword() == null) {
            return true;
        }

        return request.getPassword().equals(request.getConfirmPassword());
    }
}
