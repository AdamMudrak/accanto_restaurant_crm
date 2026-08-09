package com.example.restaurant.dto.annotations.time;

import com.example.restaurant.dto.request.management.UpdateWorkingDayRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OpenAtBeforeCloseAtValidator
        implements ConstraintValidator<OpenAtBeforeCloseAt, UpdateWorkingDayRequest> {
    @Override
    public boolean isValid(UpdateWorkingDayRequest request, ConstraintValidatorContext context) {
        if (request.openAt() == null || request.closeAt() == null) {
            return true;
        }
        return request.openAt().isBefore(request.closeAt());
    }
}
