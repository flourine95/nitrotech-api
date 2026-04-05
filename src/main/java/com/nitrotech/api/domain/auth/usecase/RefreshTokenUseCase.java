package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.dto.TokenPair;
import com.nitrotech.api.domain.auth.exception.InvalidRefreshTokenException;
import com.nitrotech.api.domain.auth.repository.RefreshTokenRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    public RefreshTokenUseCase(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository, TokenProvider tokenProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }

    public TokenPair execute(String refreshToken) {
        RefreshTokenRepository.RefreshToken token = refreshTokenRepository.findValid(refreshToken)
                .orElseThrow(InvalidRefreshTokenException::new);

        String email = userRepository.findById(token.userId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"))
                .email();

        // Rotate: revoke old, issue new
        refreshTokenRepository.revoke(refreshToken);
        String newRefreshToken = refreshTokenRepository.create(token.userId(), 30);
        String newAccessToken = tokenProvider.generate(email);

        return TokenPair.of(newAccessToken, newRefreshToken, tokenProvider.getExpirationMs());
    }
}
