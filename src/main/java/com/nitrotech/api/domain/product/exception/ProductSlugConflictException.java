package com.nitrotech.api.domain.product.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class ProductSlugConflictException extends ConflictException {

    public ProductSlugConflictException(String slug) {
        super("PRODUCT_SLUG_CONFLICT",
                "Cannot restore: slug '" + slug + "' is already used by another active product");
    }
}
