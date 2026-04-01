package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.auth.repository.EmailVerificationTokenRepository;
import com.nitrotech.api.domain.auth.repository.PasswordResetTokenRepository;
import com.nitrotech.api.infrastructure.persistence.entity.UserTokenEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserTokenRepositoryImpl implements PasswordResetTokenRepository, EmailVerificationTokenRepository {

    private final UserTokenJpaRepository jpa;

    public UserTokenRepositoryImpl(UserTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    // --- PasswordResetTokenRepository ---

    @Override
    public String create(Long userId, int expiryMinutes) {
        return createToken(userId, expiryMinutes, UserTokenEntity.Type.password_reset);
    }

    @Override
    public Optional<PasswordResetTokenRepository.ResetToken> findValid(String token) {
        return jpa.findByTokenAndType(token, UserTokenEntity.Type.password_reset)
                .filter(e -> !e.isUsed() && e.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(e -> new PasswordResetTokenRepository.ResetToken(e.getId(), e.getUserId(), e.getToken()));
    }

    @Override
    public void markUsed(String token) {
        jpa.findByTokenAndType(token, UserTokenEntity.Type.password_reset)
                .ifPresent(e -> { e.setUsed(true); jpa.save(e); });
    }

    // --- EmailVerificationTokenRepository ---

    @Override
    public String createVerification(Long userId, int expiryMinutes) {
        return createToken(userId, expiryMinutes, UserTokenEntity.Type.email_verification);
    }

    @Override
    public Optional<EmailVerificationTokenRepository.VerificationToken> findValidVerification(String token) {
        return jpa.findByTokenAndType(token, UserTokenEntity.Type.email_verification)
                .filter(e -> !e.isUsed() && e.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(e -> new EmailVerificationTokenRepository.VerificationToken(e.getId(), e.getUserId(), e.getToken()));
    }

    @Override
    public void markVerificationUsed(String token) {
        jpa.findByTokenAndType(token, UserTokenEntity.Type.email_verification)
                .ifPresent(e -> { e.setUsed(true); jpa.save(e); });
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        jpa.deleteByUserIdAndType(userId, UserTokenEntity.Type.email_verification);
    }

    // --- shared ---

    private String createToken(Long userId, int expiryMinutes, UserTokenEntity.Type type) {
        UserTokenEntity entity = new UserTokenEntity();
        entity.setUserId(userId);
        entity.setToken(UUID.randomUUID().toString());
        entity.setType(type);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        return jpa.save(entity).getToken();
    }
}
