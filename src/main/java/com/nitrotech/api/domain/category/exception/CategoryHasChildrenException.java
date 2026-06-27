package com.nitrotech.api.domain.category.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class CategoryHasChildrenException extends ConflictException {

    public CategoryHasChildrenException(String message) {
        super("CATEGORY_HAS_CHILDREN", message);
    }
}
