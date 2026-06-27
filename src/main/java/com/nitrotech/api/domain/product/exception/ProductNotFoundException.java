package com.nitrotech.api.domain.product.exception;

import com.nitrotech.api.shared.exception.NotFoundException;

public class ProductNotFoundException extends NotFoundException {

    public ProductNotFoundException() {
        super("PRODUCT_NOT_FOUND", "Product not found");
    }

    private ProductNotFoundException(String message) {
        super("PRODUCT_NOT_FOUND", message);
    }

    public static ProductNotFoundException deleted() {
        return new ProductNotFoundException("Deleted product not found");
    }

    public static ProductNotFoundException deletedForHardDelete() {
        return new ProductNotFoundException("Deleted product not found. Soft delete first before permanent delete.");
    }

    public static ProductNotFoundException withIdOrSlug(String idOrSlug) {
        return new ProductNotFoundException("Product '" + idOrSlug + "' not found");
    }

    public static ProductNotFoundException withId(Long id) {
        return new ProductNotFoundException("Product with ID " + id + " not found");
    }

    public static ProductNotFoundException withSlug(String slug) {
        return new ProductNotFoundException("Product with slug '" + slug + "' not found");
    }
}
