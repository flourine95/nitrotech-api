package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.repository.EmailVerificationTokenRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResendVerificationUseCase {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final EmailSender emailSender;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public ResendVerificationUseCase(UserRepository userRepository,
                                      EmailVerificationTokenRepository verificationTokenRepository,
                                      EmailSender emailSender) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailSender = emailSender;
    }

    public void execute(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        // Xóa token cũ, tạo token mới
        verificationTokenRepository.deleteByUserId(user.id());
        String token = verificationTokenRepository.createVerification(user.id(), 24 * 60);
        String verifyLink = frontendUrl + "/verify-email?token=" + token;
        emailSender.sendVerificationEmail(email, verifyLink);
    }
}
