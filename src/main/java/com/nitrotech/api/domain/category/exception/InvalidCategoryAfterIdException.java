package com.nitrotech.api.domain.category.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class InvalidCategoryAfterIdException extends ConflictException {

    public InvalidCategoryAfterIdException() {
        super("INVALID_AFTER_ID", "afterId category not found");
    }
}
