package com.nitrotech.api.domain.auth.exception;

import com.nitrotech.api.shared.exception.BadRequestException;

public class OAuthAccountAlreadyLinkedException extends BadRequestException {

    public OAuthAccountAlreadyLinkedException(String provider) {
        super("OAUTH_ACCOUNT_ALREADY_LINKED", "Another " + provider + " account is already linked to this user");
    }
}
