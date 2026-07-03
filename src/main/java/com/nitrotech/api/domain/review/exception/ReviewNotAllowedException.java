package com.nitrotech.api.domain.review.exception;

import com.nitrotech.api.shared.exception.ForbiddenException;

public class ReviewNotAllowedException extends ForbiddenException {

    public ReviewNotAllowedException() {
        super("REVIEW_NOT_ALLOWED", "You can only review products from your delivered order");
    }
}
