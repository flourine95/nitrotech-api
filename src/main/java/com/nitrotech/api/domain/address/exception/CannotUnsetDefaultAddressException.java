package com.nitrotech.api.domain.address.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class CannotUnsetDefaultAddressException extends DomainException {

    public CannotUnsetDefaultAddressException() {
        super("CANNOT_UNSET_DEFAULT_ADDRESS",
                "Cannot unset the default address. Set another address as default first.");
    }
}
