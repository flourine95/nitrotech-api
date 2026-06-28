package com.nitrotech.api.domain.product.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class VariantSkuExistsException extends ConflictException {

    public VariantSkuExistsException() {
        super("VARIANT_SKU_EXISTS", "SKU already exists");
    }

    public VariantSkuExistsException(String sku) {
        super("VARIANT_SKU_EXISTS", "SKU already exists: " + sku);
    }
}
