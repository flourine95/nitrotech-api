package com.nitrotech.api.domain.auth.exception;

import com.nitrotech.api.shared.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException() {
        super("USER_NOT_FOUND", "User not found");
    }
}
