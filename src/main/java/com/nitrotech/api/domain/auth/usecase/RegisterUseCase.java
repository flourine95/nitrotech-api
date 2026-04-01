package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.RegisterCommand;
import com.nitrotech.api.domain.auth.exception.EmailAlreadyExistsException;
import com.nitrotech.api.domain.auth.repository.EmailVerificationTokenRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final EmailSender emailSender;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public RegisterUseCase(UserRepository userRepository,
                           PasswordEncoder passwordEncoder, TokenProvider tokenProvider,
                           EmailVerificationTokenRepository verificationTokenRepository,
                           EmailSender emailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailSender = emailSender;
    }

    public AuthResult execute(RegisterCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException(command.email());
        }

        String hashed = passwordEncoder.encode(command.password());
        AuthResult.UserData user = userRepository.save(command.name(), command.email(), hashed);

        // Gửi email xác thực (async), token hết hạn sau 24 giờ
        String token = verificationTokenRepository.createVerification(user.id(), 24 * 60);
        String verifyLink = frontendUrl + "/verify-email?token=" + token;
        emailSender.sendVerificationEmail(user.email(), verifyLink);

        return AuthResult.ofUser(user);
    }
}
