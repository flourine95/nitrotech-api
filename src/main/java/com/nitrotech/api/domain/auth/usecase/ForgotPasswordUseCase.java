package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.exception.EmailNotFoundException;
import com.nitrotech.api.domain.auth.repository.PasswordResetTokenRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ForgotPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailSender emailSender;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public void execute(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailNotFoundException(email));

        String token = resetTokenRepository.create(user.id(), 15);
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        emailSender.sendPasswordReset(email, resetLink);
    }
}
