package com.nitrotech.api.domain.auth.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class InvalidVerificationTokenException extends DomainException {
    public InvalidVerificationTokenException() {
        super("INVALID_VERIFICATION_TOKEN", "Verification token is invalid or has expired");
    }
}
