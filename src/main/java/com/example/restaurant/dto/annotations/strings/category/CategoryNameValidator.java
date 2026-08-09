package com.example.restaurant.dto.annotations.strings.category;

import com.example.restaurant.dto.request.menu.MenuCategoryRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CategoryNameValidator
        implements ConstraintValidator<CategoryName, MenuCategoryRequest> {

    @Override
    public boolean isValid(MenuCategoryRequest request,
                           ConstraintValidatorContext constraintValidatorContext) {
        boolean hasDefaultName = request.nameDefaultLanguage() != null
                && !request.nameDefaultLanguage().isBlank();
        boolean hasSecondName = request.nameSecondLanguage() != null
                && !request.nameSecondLanguage().isBlank();

        return hasDefaultName || hasSecondName;
    }
}
