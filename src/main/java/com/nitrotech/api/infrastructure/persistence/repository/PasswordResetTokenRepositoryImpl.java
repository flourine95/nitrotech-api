package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.auth.repository.PasswordResetTokenRepository;
import com.nitrotech.api.infrastructure.persistence.entity.PasswordResetTokenEntity;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpa;

    public PasswordResetTokenRepositoryImpl(PasswordResetTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public String create(Long userId, int expiryMinutes) {
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        entity.setUserId(userId);
        entity.setToken(UUID.randomUUID().toString());
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        return jpa.save(entity).getToken();
    }

    @Override
    public Optional<ResetToken> findValid(String token) {
        return jpa.findByToken(token)
                .filter(e -> !e.isUsed() && e.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(e -> new ResetToken(e.getId(), e.getUserId(), e.getToken()));
    }

    @Override
    public void markUsed(String token) {
        jpa.findByToken(token).ifPresent(e -> {
            e.setUsed(true);
            jpa.save(e);
        });
    }
}
