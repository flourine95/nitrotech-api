package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.RegisterCommand;
import com.nitrotech.api.domain.auth.exception.EmailAlreadyExistsException;
import com.nitrotech.api.domain.auth.repository.EmailVerificationTokenRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.domain.notification.service.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RegisterUseCaseTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private EmailVerificationTokenRepository verificationTokenRepository;
    private EmailSender emailSender;
    private NotificationPublisher notificationPublisher;
    private RegisterUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        verificationTokenRepository = mock(EmailVerificationTokenRepository.class);
        emailSender = mock(EmailSender.class);
        notificationPublisher = mock(NotificationPublisher.class);
        useCase = new RegisterUseCase(
                userRepository,
                passwordEncoder,
                verificationTokenRepository,
                emailSender,
                notificationPublisher
        );
    }

    @Test
    void rejectsExistingEmailBeforeCreatingUser() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new RegisterCommand("User", "user@example.com", "secret")))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(anyString(), anyString(), anyString());
        verifyNoInteractions(emailSender, notificationPublisher);
    }

    @Test
    void createsUserAndSendsVerificationWhenEmailIsAvailable() {
        when(passwordEncoder.encode("secret")).thenReturn("hash");
        when(userRepository.save("User", "user@example.com", "hash"))
                .thenReturn(new AuthResult.UserData(1L, "User", "user@example.com", Set.of("customer"), Set.of()));
        when(verificationTokenRepository.createVerification(1L, 24 * 60)).thenReturn("token");

        AuthResult result = useCase.execute(new RegisterCommand("User", "user@example.com", "secret"));

        assertThat(result.user().email()).isEqualTo("user@example.com");
        verify(emailSender).sendVerificationEmail(eq("user@example.com"), contains("/verify-email?token=token"));
        verify(notificationPublisher).publish(any());
    }
}
