package com.nitrotech.api.domain.access.exception;

import com.nitrotech.api.shared.exception.ForbiddenException;

public class SelfLockoutGuardException extends ForbiddenException {

    public SelfLockoutGuardException(String message) {
        super("SELF_LOCKOUT_GUARD", message);
    }
}
