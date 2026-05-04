package com.nitrotech.api.domain.address.exception;

public class AddressAccessDeniedException extends RuntimeException {

    public AddressAccessDeniedException() {
        super("This address does not belong to you");
    }
}
