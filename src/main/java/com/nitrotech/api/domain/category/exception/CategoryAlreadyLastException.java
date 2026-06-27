package com.nitrotech.api.domain.category.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class CategoryAlreadyLastException extends ConflictException {

    public CategoryAlreadyLastException() {
        super("ALREADY_LAST", "Category is already at the last position");
    }
}
