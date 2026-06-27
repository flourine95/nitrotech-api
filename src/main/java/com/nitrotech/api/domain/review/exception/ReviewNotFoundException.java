package com.nitrotech.api.domain.review.exception;

import com.nitrotech.api.shared.exception.NotFoundException;

public class ReviewNotFoundException extends NotFoundException {

    public ReviewNotFoundException() {
        super("REVIEW_NOT_FOUND", "Review not found");
    }
}
