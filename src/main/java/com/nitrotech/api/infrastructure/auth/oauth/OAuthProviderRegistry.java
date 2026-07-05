package com.nitrotech.api.infrastructure.auth.oauth;

import com.nitrotech.api.domain.auth.provider.OAuthProvider;
import com.nitrotech.api.domain.auth.provider.OAuthProviderResolver;
import com.nitrotech.api.shared.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuthProviderRegistry implements OAuthProviderResolver {

    private final Map<String, OAuthProvider> providers;

    public OAuthProviderRegistry(List<OAuthProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        provider -> provider.getProviderName().toLowerCase(),
                        Function.identity()
                ));
    }

    @Override
    public OAuthProvider getProvider(String name) {
        return Optional.ofNullable(name)
                .map(String::toLowerCase)
                .map(providers::get)
                .orElseThrow(() -> new BadRequestException(
                        "INVALID_OAUTH_PROVIDER",
                        "OAuth provider '" + name + "' is not supported."
                ));
    }
}
