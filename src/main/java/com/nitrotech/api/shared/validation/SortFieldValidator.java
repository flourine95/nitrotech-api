package com.nitrotech.api.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public class SortFieldValidator implements ConstraintValidator<ValidSortFields, Pageable> {

    private Set<String> allowedFields;

    @Override
    public void initialize(ValidSortFields annotation) {
        this.allowedFields = Set.of(annotation.value());
    }

    @Override
    public boolean isValid(Pageable pageable, ConstraintValidatorContext context) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return true;
        }

        for (var order : pageable.getSort()) {
            if (!allowedFields.contains(order.getProperty())) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "Sort field not allowed: " + order.getProperty()
                ).addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
