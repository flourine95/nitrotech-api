package com.nitrotech.api.domain.auth.repository;

import java.util.Optional;

public interface RefreshTokenRepository {
    String create(Long userId, int expiryDays);
    Optional<RefreshToken> findValid(String token);
    void revoke(String token);
    void revokeAllByUserId(Long userId);

    record RefreshToken(Long id, Long userId, String token) {}
}
