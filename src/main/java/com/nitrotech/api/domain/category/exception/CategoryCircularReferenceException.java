package com.nitrotech.api.domain.category.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class CategoryCircularReferenceException extends ConflictException {

    public CategoryCircularReferenceException(String message) {
        super("CATEGORY_CIRCULAR_REF", message);
    }
}
