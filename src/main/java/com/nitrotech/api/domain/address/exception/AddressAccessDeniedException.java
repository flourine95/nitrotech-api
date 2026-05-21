package com.nitrotech.api.domain.address.exception;

import com.nitrotech.api.shared.exception.ForbiddenException;

public class AddressAccessDeniedException extends ForbiddenException {

    public AddressAccessDeniedException() {
        super("ADDRESS_ACCESS_DENIED", "This address does not belong to you");
    }
}
