package com.nitrotech.api.domain.brand.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class BrandSlugExistsException extends ConflictException {

    public BrandSlugExistsException() {
        super("BRAND_SLUG_EXISTS", "Slug already exists");
    }
}
