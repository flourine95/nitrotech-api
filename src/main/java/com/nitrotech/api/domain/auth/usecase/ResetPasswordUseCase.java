package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.exception.InvalidResetTokenException;
import com.nitrotech.api.domain.auth.repository.PasswordResetTokenRepository;
import com.nitrotech.api.domain.auth.repository.RefreshTokenRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ResetPasswordUseCase {

    private final PasswordResetTokenRepository resetTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public ResetPasswordUseCase(PasswordResetTokenRepository resetTokenRepository,
                                 UserRepository userRepository,
                                 RefreshTokenRepository refreshTokenRepository,
                                 PasswordEncoder passwordEncoder) {
        this.resetTokenRepository = resetTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void execute(String token, String newPassword) {
        PasswordResetTokenRepository.ResetToken resetToken = resetTokenRepository.findValid(token)
                .orElseThrow(InvalidResetTokenException::new);

        userRepository.updatePassword(resetToken.userId(), passwordEncoder.encode(newPassword));
        resetTokenRepository.markUsed(token);
        // Revoke tất cả refresh tokens — bắt login lại trên mọi thiết bị
        refreshTokenRepository.revokeAllByUserId(resetToken.userId());
    }
}
