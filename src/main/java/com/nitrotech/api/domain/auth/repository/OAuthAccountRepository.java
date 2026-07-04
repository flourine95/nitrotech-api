package com.nitrotech.api.domain.auth.repository;

import com.nitrotech.api.domain.auth.dto.OAuthUserInfo;

import java.util.Optional;

public interface OAuthAccountRepository {
    Optional<OAuthAccountLink> findByProviderAndExternalId(String provider, String externalId);

    Optional<OAuthAccountLink> findByProviderAndUserId(String provider, Long userId);

    void saveOrUpdate(Long userId, String provider, OAuthUserInfo userInfo);

    record OAuthAccountLink(Long userId, String provider, String externalId) {
    }
}
