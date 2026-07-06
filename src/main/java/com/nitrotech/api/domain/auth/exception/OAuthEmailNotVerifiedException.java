package com.nitrotech.api.domain.auth.exception;

import com.nitrotech.api.shared.exception.BadRequestException;

public class OAuthEmailNotVerifiedException extends BadRequestException {

    public OAuthEmailNotVerifiedException() {
        super("OAUTH_EMAIL_NOT_VERIFIED", "OAuth provider email must be verified");
    }
}
