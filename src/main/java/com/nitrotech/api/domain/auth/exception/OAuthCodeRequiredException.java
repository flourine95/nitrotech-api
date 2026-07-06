package com.nitrotech.api.domain.auth.exception;

import com.nitrotech.api.shared.exception.BadRequestException;

public class OAuthCodeRequiredException extends BadRequestException {

    public OAuthCodeRequiredException() {
        super("OAUTH_CODE_REQUIRED", "Authorization code is required");
    }
}
