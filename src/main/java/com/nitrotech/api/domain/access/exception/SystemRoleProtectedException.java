package com.nitrotech.api.domain.access.exception;

import com.nitrotech.api.shared.exception.ForbiddenException;

public class SystemRoleProtectedException extends ForbiddenException {

    public SystemRoleProtectedException() {
        super("SYSTEM_ROLE_PROTECTED", "System roles cannot be edited");
    }
}
