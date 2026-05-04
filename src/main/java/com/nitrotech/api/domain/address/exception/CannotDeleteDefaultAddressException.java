package com.nitrotech.api.domain.address.exception;

public class CannotDeleteDefaultAddressException extends RuntimeException {

    public CannotDeleteDefaultAddressException() {
        super("Cannot delete default address. Set another address as default first.");
    }
}
