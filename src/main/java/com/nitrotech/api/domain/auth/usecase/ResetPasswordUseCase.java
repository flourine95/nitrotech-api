package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.exception.InvalidResetTokenException;
import com.nitrotech.api.domain.auth.repository.PasswordResetTokenRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

@Service
public class ResetPasswordUseCase {

    private final PasswordResetTokenRepository resetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public ResetPasswordUseCase(PasswordResetTokenRepository resetTokenRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.resetTokenRepository = resetTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRepository = sessionRepository;
    }

    public void execute(String token, String newPassword) {
        PasswordResetTokenRepository.ResetToken resetToken = resetTokenRepository.findValid(token)
                .orElseThrow(InvalidResetTokenException::new);

        userRepository.updatePassword(resetToken.userId(), passwordEncoder.encode(newPassword));
        resetTokenRepository.markUsed(token);

        // Invalidate tất cả session của user — force login lại trên mọi thiết bị
        userRepository.findById(resetToken.userId()).ifPresent(user ->
            sessionRepository.findByPrincipalName(user.email())
                .values()
                .forEach(s -> sessionRepository.deleteById(s.getId()))
        );
    }
}
