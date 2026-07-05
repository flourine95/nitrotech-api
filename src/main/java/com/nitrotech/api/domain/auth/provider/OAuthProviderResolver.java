package com.nitrotech.api.domain.auth.provider;

public interface OAuthProviderResolver {
    OAuthProvider getProvider(String name);
}
