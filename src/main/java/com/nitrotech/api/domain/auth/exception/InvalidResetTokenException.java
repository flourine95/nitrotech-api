package com.nitrotech.api.domain.auth.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class InvalidResetTokenException extends DomainException {

    public InvalidResetTokenException() {
        super("INVALID_RESET_TOKEN", "Password reset token is invalid or expired");
    }
}
