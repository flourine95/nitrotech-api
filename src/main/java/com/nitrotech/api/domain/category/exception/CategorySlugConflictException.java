package com.nitrotech.api.domain.category.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class CategorySlugConflictException extends ConflictException {

    public CategorySlugConflictException(String slug) {
        super("CATEGORY_SLUG_CONFLICT",
                "Cannot restore: slug '" + slug + "' is already used by another category");
    }
}
