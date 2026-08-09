package com.example.restaurant.dto.annotations.password;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = PasswordValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {
    String message() default "bad password. Should contain at least 8 characters, "
            + "at least 1 upper case, 1 lower case, 1 special character, 1 digit";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
