package com.nitrotech.api.domain.auth.exception;

import com.nitrotech.api.shared.exception.NotFoundException;

public class EmailNotFoundException extends NotFoundException {

    public EmailNotFoundException(String email) {
        super("EMAIL_NOT_FOUND", "No account found with email " + email);
    }
}
