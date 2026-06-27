package com.nitrotech.api.domain.product.exception;

import com.nitrotech.api.shared.exception.NotFoundException;

public class VariantNotFoundException extends NotFoundException {

    public VariantNotFoundException() {
        super("VARIANT_NOT_FOUND", "Variant not found");
    }
}
