package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.exception.InvalidResetTokenException;
import com.nitrotech.api.domain.auth.repository.PasswordResetTokenRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase {

    private final PasswordResetTokenRepository resetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    @Transactional
    public void execute(String token, String newPassword) {
        PasswordResetTokenRepository.ResetToken resetToken = resetTokenRepository.findValid(token)
                .orElseThrow(InvalidResetTokenException::new);

        userRepository.updatePassword(resetToken.userId(), passwordEncoder.encode(newPassword));
        resetTokenRepository.markUsed(token);

        userRepository.findById(resetToken.userId()).ifPresent(user ->
            sessionRepository.findByPrincipalName(user.email())
                    .values()
                    .forEach(s -> sessionRepository.deleteById(s.getId()))
        );
    }
}
