package com.nitrotech.api.domain.address.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class DefaultAddressConflictException extends ConflictException {

    public DefaultAddressConflictException() {
        super("DEFAULT_ADDRESS_CONFLICT", "Another address was set as default. Please try again.");
    }
}
