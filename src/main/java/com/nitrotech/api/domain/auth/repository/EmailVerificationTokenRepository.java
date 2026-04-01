package com.nitrotech.api.domain.auth.repository;

import java.util.Optional;

public interface EmailVerificationTokenRepository {
    String createVerification(Long userId, int expiryMinutes);
    Optional<VerificationToken> findValidVerification(String token);
    void markVerificationUsed(String token);
    void deleteByUserId(Long userId);

    record VerificationToken(Long id, Long userId, String token) {}
}
