package com.nitrotech.api.domain.review.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class ReviewAlreadyExistsException extends ConflictException {

    public ReviewAlreadyExistsException() {
        super("REVIEW_ALREADY_EXISTS", "You have already reviewed this product for this order");
    }
}
