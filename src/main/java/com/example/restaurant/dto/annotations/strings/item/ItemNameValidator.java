package com.example.restaurant.dto.annotations.strings.item;

import com.example.restaurant.dto.request.menu.MenuItemRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ItemNameValidator
        implements ConstraintValidator<ItemName, MenuItemRequest> {

    @Override
    public boolean isValid(MenuItemRequest request,
                           ConstraintValidatorContext constraintValidatorContext) {
        boolean hasDefaultName = request.nameDefaultLanguage() != null
                && !request.nameDefaultLanguage().isBlank();
        boolean hasSecondName = request.nameSecondLanguage() != null
                && !request.nameSecondLanguage().isBlank();

        return hasDefaultName || hasSecondName;
    }
}
