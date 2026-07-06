package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.provider.OAuthProviderResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthAuthorizeUseCase {

    private final OAuthProviderResolver oauthProviderResolver;

    public String execute(String provider, String state) {
        return oauthProviderResolver.getProvider(provider).buildAuthorizationUrl(state);
    }
}
