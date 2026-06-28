package com.nitrotech.api.domain.brand.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class BrandHasProductsException extends ConflictException {

    public BrandHasProductsException() {
        super("BRAND_HAS_PRODUCTS", "Cannot permanently delete brand that has products.");
    }
}
