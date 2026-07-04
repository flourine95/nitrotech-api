package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.OAuthAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthAccountJpaRepository extends JpaRepository<OAuthAccountEntity, Long> {
    Optional<OAuthAccountEntity> findByProviderIgnoreCaseAndExternalId(String provider, String externalId);

    Optional<OAuthAccountEntity> findByProviderIgnoreCaseAndUserId(String provider, Long userId);
}
