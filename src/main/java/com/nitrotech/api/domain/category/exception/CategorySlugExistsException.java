package com.nitrotech.api.domain.category.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class CategorySlugExistsException extends ConflictException {

    public CategorySlugExistsException(String slug) {
        super("CATEGORY_SLUG_EXISTS", "Slug '" + slug + "' already exists");
    }
}
