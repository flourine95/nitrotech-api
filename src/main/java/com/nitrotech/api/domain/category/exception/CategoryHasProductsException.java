package com.nitrotech.api.domain.category.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class CategoryHasProductsException extends ConflictException {

    public CategoryHasProductsException() {
        super("CATEGORY_HAS_PRODUCTS", "Cannot permanently delete category that has products.");
    }
}
