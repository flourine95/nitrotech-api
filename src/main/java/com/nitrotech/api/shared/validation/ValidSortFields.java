package com.nitrotech.api.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SortFieldValidator.class)
public @interface ValidSortFields {
    String[] value();
    String message() default "Invalid sort field";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
