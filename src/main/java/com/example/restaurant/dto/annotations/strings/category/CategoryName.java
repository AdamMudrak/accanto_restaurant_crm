package com.example.restaurant.dto.annotations.strings.category;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = CategoryNameValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CategoryName {
    String message() default "nameDefaultLanguage or nameSecondLanguage required";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
