package com.example.restaurant.dto.annotations.phone;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = PhoneValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Phone {
    String message() default "bad phone number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
