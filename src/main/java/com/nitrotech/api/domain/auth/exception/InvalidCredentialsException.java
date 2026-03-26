package com.nitrotech.api.domain.auth.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Email or password is incorrect");
    }
}
