package com.nitrotech.api.domain.category.exception;

import com.nitrotech.api.shared.exception.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {

    public CategoryNotFoundException() {
        super("CATEGORY_NOT_FOUND", "Category not found");
    }

    public CategoryNotFoundException(String message) {
        super("CATEGORY_NOT_FOUND", message);
    }

    public static CategoryNotFoundException withId(Long id) {
        return new CategoryNotFoundException("Category with ID " + id + " not found");
    }

    public static CategoryNotFoundException withSlug(String slug) {
        return new CategoryNotFoundException("Category with slug '" + slug + "' not found");
    }

    public static CategoryNotFoundException parentWithId(Long id) {
        return new CategoryNotFoundException("Parent category with ID " + id + " not found");
    }

    public static CategoryNotFoundException deletedForHardDelete() {
        return new CategoryNotFoundException("Deleted category not found. Soft delete first before permanent delete.");
    }
}
