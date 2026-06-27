package com.nitrotech.api.domain.brand.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class BrandSlugConflictException extends ConflictException {

    public BrandSlugConflictException(String slug) {
        super("BRAND_SLUG_CONFLICT",
                "Cannot restore: slug '" + slug + "' is already used by another active brand");
    }
}
