package com.nitrotech.api.domain.access.exception;

import com.nitrotech.api.shared.exception.BadRequestException;

public class RoleReferenceNotFoundException extends BadRequestException {

    public RoleReferenceNotFoundException() {
        super("ROLE_NOT_FOUND", "One or more roles were not found");
    }
}
