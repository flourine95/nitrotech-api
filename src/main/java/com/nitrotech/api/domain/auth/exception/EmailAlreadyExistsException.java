package com.nitrotech.api.domain.auth.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class EmailAlreadyExistsException extends ConflictException {

    public EmailAlreadyExistsException(String email) {
        super("EMAIL_ALREADY_EXISTS", "Email " + email + " is already registered");
    }
}
