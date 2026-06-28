package com.nitrotech.api.domain.category.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class CategoryAlreadyFirstException extends ConflictException {

    public CategoryAlreadyFirstException() {
        super("ALREADY_FIRST", "Category is already at the first position");
    }
}
