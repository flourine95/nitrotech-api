package com.nitrotech.api.domain.product.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class ProductSlugExistsException extends ConflictException {

    public ProductSlugExistsException() {
        super("PRODUCT_SLUG_EXISTS", "Slug already exists");
    }
}
