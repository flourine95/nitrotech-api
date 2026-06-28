package com.nitrotech.api.domain.access.exception;

import com.nitrotech.api.shared.exception.NotFoundException;

public class RoleNotFoundException extends NotFoundException {

    public RoleNotFoundException() {
        super("ROLE_NOT_FOUND", "Role not found");
    }
}
