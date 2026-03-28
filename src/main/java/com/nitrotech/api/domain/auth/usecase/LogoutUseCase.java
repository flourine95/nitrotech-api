package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

@Service
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenRevoker accessTokenRevoker;

    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository, AccessTokenRevoker accessTokenRevoker) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenRevoker = accessTokenRevoker;
    }

    public void execute(String refreshToken, String accessToken) {
        refreshTokenRepository.revoke(refreshToken);
        if (accessToken != null) {
            accessTokenRevoker.revoke(accessToken);
        }
    }

    public void executeAll(Long userId, String accessToken) {
        refreshTokenRepository.revokeAllByUserId(userId);
        if (accessToken != null) {
            accessTokenRevoker.revoke(accessToken);
        }
    }
}
