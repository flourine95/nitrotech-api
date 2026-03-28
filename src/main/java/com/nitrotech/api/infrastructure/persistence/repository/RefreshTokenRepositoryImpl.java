package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.auth.repository.RefreshTokenRepository;
import com.nitrotech.api.infrastructure.persistence.entity.RefreshTokenEntity;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    public RefreshTokenRepositoryImpl(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public String create(Long userId, int expiryDays) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(userId);
        entity.setToken(UUID.randomUUID().toString());
        entity.setExpiresAt(LocalDateTime.now().plusDays(expiryDays));
        return jpa.save(entity).getToken();
    }

    @Override
    public Optional<RefreshToken> findValid(String token) {
        return jpa.findByToken(token)
                .filter(e -> !e.isRevoked() && e.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(e -> new RefreshToken(e.getId(), e.getUserId(), e.getToken()));
    }

    @Override
    @Transactional
    public void revoke(String token) {
        jpa.findByToken(token).ifPresent(e -> {
            e.setRevoked(true);
            jpa.save(e);
        });
    }

    @Override
    @Transactional
    public void revokeAllByUserId(Long userId) {
        jpa.revokeAllByUserId(userId);
    }
}
