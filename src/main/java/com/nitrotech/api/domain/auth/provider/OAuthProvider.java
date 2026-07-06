package com.nitrotech.api.domain.auth.provider;

import com.nitrotech.api.domain.auth.dto.OAuthTokenResponse;
import com.nitrotech.api.domain.auth.dto.OAuthUserInfo;

public interface OAuthProvider {
    String getProviderName();

    String buildAuthorizationUrl(String state);

    OAuthTokenResponse exchangeAuthorizationCode(String code);

    OAuthUserInfo fetchUserInfo(OAuthTokenResponse tokenResponse);
}
