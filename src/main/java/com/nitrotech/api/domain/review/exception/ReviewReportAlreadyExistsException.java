package com.nitrotech.api.domain.review.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class ReviewReportAlreadyExistsException extends ConflictException {

    public ReviewReportAlreadyExistsException() {
        super("REVIEW_REPORT_ALREADY_EXISTS", "You have already reported this review");
    }
}
