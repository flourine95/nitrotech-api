package com.nitrotech.api.domain.auth.exception;

import com.nitrotech.api.shared.exception.BadRequestException;

public class OAuthExternalIdRequiredException extends BadRequestException {

    public OAuthExternalIdRequiredException() {
        super("OAUTH_EXTERNAL_ID_REQUIRED", "OAuth provider did not return an external user ID");
    }
}
