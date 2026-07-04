package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.RegisterCommand;
import com.nitrotech.api.domain.auth.exception.EmailAlreadyExistsException;
import com.nitrotech.api.domain.notification.dto.NotificationEvent;
import com.nitrotech.api.domain.notification.service.NotificationPublisher;
import com.nitrotech.api.domain.auth.repository.EmailVerificationTokenRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final EmailSender emailSender;
    private final NotificationPublisher notificationPublisher;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public AuthResult execute(RegisterCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException(command.email());
        }

        String hashed = passwordEncoder.encode(command.password());
        AuthResult.UserData user = userRepository.save(command.name(), command.email(), hashed);

        String token = verificationTokenRepository.createVerification(user.id(), 24 * 60);
        String verifyLink = frontendUrl + "/verify-email?token=" + token;
        emailSender.sendVerificationEmail(user.email(), verifyLink);
        notificationPublisher.publish(new NotificationEvent(
                null,
                "USER_SIGNUP",
                "Người dùng mới đăng ký",
                user.name() + " (" + user.email() + ") vừa tạo tài khoản.",
                "/dashboard/access",
                null,
                null,
                "USER_READ"
        ));

        return AuthResult.ofUser(user);
    }
}
