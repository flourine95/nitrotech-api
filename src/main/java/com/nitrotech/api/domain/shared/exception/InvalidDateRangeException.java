package com.nitrotech.api.domain.shared.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class InvalidDateRangeException extends DomainException {

    public InvalidDateRangeException() {
        super("INVALID_DATE_RANGE", "Start date must be before end date");
    }
}
