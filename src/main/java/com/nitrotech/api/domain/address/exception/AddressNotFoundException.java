package com.nitrotech.api.domain.address.exception;

public class AddressNotFoundException extends RuntimeException {

    private AddressNotFoundException(String message) {
        super(message);
    }

    public static AddressNotFoundException withId(Long id) {
        return new AddressNotFoundException("Address with ID " + id + " not found");
    }
}
