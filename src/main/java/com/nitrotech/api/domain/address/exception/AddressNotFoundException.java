package com.nitrotech.api.domain.address.exception;

import com.nitrotech.api.shared.exception.NotFoundException;

public class AddressNotFoundException extends NotFoundException {

    private AddressNotFoundException(String message) {
        super("ADDRESS_NOT_FOUND", message);
    }

    public static AddressNotFoundException withId(Long id) {
        return new AddressNotFoundException("Address with ID " + id + " not found");
    }
}
