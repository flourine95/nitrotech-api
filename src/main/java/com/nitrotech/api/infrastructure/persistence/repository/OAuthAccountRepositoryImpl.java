package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.auth.dto.OAuthUserInfo;
import com.nitrotech.api.domain.auth.repository.OAuthAccountRepository;
import com.nitrotech.api.infrastructure.persistence.entity.OAuthAccountEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OAuthAccountRepositoryImpl implements OAuthAccountRepository {

    private final OAuthAccountJpaRepository jpa;

    @Override
    public Optional<OAuthAccountLink> findByProviderAndExternalId(String provider, String externalId) {
        return jpa.findByProviderIgnoreCaseAndExternalId(provider, externalId)
                .map(entity -> new OAuthAccountLink(entity.getUserId(), entity.getProvider(), entity.getExternalId()));
    }

    @Override
    public Optional<OAuthAccountLink> findByProviderAndUserId(String provider, Long userId) {
        return jpa.findByProviderIgnoreCaseAndUserId(provider, userId)
                .map(entity -> new OAuthAccountLink(entity.getUserId(), entity.getProvider(), entity.getExternalId()));
    }

    @Override
    @Transactional
    public void saveOrUpdate(Long userId, String provider, OAuthUserInfo userInfo) {
        OAuthAccountEntity entity = jpa.findByProviderIgnoreCaseAndExternalId(provider, userInfo.externalId())
                .orElseGet(OAuthAccountEntity::new);
        entity.setUserId(userId);
        entity.setProvider(provider.toLowerCase());
        entity.setExternalId(userInfo.externalId());
        entity.setEmail(userInfo.email());
        entity.setDisplayName(userInfo.name());
        entity.setAvatarUrl(userInfo.avatar());
        jpa.save(entity);
    }
}
