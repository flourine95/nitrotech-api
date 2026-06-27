package com.nitrotech.api.domain.brand.exception;

import com.nitrotech.api.shared.exception.NotFoundException;

public class BrandNotFoundException extends NotFoundException {

    public BrandNotFoundException() {
        super("BRAND_NOT_FOUND", "Brand not found");
    }

    private BrandNotFoundException(String message) {
        super("BRAND_NOT_FOUND", message);
    }

    public static BrandNotFoundException deleted() {
        return new BrandNotFoundException("Deleted brand not found");
    }

    public static BrandNotFoundException deletedForHardDelete() {
        return new BrandNotFoundException("Deleted brand not found. Soft delete first before permanent delete.");
    }

    public static BrandNotFoundException withId(Long id) {
        return new BrandNotFoundException("Brand with ID " + id + " not found");
    }

    public static BrandNotFoundException withSlug(String slug) {
        return new BrandNotFoundException("Brand with slug '" + slug + "' not found");
    }
}
