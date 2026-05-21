package com.nitrotech.api.domain.address.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class CannotDeleteDefaultAddressException extends DomainException {

    public CannotDeleteDefaultAddressException() {
        super("CANNOT_DELETE_DEFAULT_ADDRESS", 
              "Cannot delete default address. Set another address as default first.");
    }
}
