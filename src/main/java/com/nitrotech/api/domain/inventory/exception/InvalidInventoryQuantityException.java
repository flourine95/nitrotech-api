package com.nitrotech.api.domain.inventory.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class InvalidInventoryQuantityException extends DomainException {

    public InvalidInventoryQuantityException() {
        super("INVALID_QUANTITY", "Quantity cannot be negative");
    }
}
