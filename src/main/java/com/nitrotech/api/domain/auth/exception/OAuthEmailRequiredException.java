package com.nitrotech.api.domain.auth.exception;

import com.nitrotech.api.shared.exception.BadRequestException;

public class OAuthEmailRequiredException extends BadRequestException {

    public OAuthEmailRequiredException() {
        super("OAUTH_EMAIL_REQUIRED", "OAuth provider did not return an email address");
    }
}
