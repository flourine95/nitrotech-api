package com.nitrotech.api.domain.auth.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class InvalidRefreshTokenException extends DomainException {

    public InvalidRefreshTokenException() {
        super("INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired");
    }
}
