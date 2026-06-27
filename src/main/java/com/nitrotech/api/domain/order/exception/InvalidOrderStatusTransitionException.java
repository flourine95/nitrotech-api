package com.nitrotech.api.domain.order.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class InvalidOrderStatusTransitionException extends DomainException {

    public InvalidOrderStatusTransitionException(String currentStatus, String newStatus) {
        super("INVALID_STATUS_TRANSITION", "Cannot transition from " + currentStatus + " to " + newStatus);
    }
}
