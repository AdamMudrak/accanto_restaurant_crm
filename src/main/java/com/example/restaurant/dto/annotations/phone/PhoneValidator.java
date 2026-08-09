package com.example.restaurant.dto.annotations.phone;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PhoneValidator implements ConstraintValidator<Phone, String> {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext constraintValidatorContext) {
        return phone == null || PHONE_PATTERN.matcher(phone).matches();
    }
}
