package com.example.restaurant.dto.annotations.time;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = OpenAtBeforeCloseAtValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpenAtBeforeCloseAt {
    String message() default "openAt must be earlier than closeAt.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
