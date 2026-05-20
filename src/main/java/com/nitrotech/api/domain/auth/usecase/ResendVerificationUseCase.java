package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.repository.EmailVerificationTokenRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResendVerificationUseCase {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final EmailSender emailSender;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public void execute(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        verificationTokenRepository.deleteByUserId(user.id());
        String token = verificationTokenRepository.createVerification(user.id(), 24 * 60);
        String verifyLink = frontendUrl + "/verify-email?token=" + token;
        emailSender.sendVerificationEmail(email, verifyLink);
    }
}
