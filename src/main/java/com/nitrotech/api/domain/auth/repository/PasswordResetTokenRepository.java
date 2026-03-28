package com.nitrotech.api.domain.auth.repository;

import java.util.Optional;

public interface PasswordResetTokenRepository {
    String create(Long userId, int expiryMinutes);
    Optional<ResetToken> findValid(String token);
    void markUsed(String token);

    record ResetToken(Long id, Long userId, String token) {}
}
