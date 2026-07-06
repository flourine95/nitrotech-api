package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.LoginCommand;
import com.nitrotech.api.domain.auth.exception.AccountNotActiveException;
import com.nitrotech.api.domain.auth.exception.InvalidCredentialsException;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LoginUseCaseTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private LoginUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        useCase = new LoginUseCase(userRepository, passwordEncoder);
    }

    @Test
    void rejectsBannedAccountEvenWithValidPassword() {
        when(userRepository.findCredentialByEmail("user@example.com"))
                .thenReturn(Optional.of(new UserRepository.UserCredential(1L, "User", "user@example.com", "hash", "banned")));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new LoginCommand("user@example.com", "secret")))
                .isInstanceOf(AccountNotActiveException.class);

        verify(userRepository, never()).findAuthoritiesByUserId(anyLong());
    }

    @Test
    void logsInActiveAccountWithValidPassword() {
        when(userRepository.findCredentialByEmail("user@example.com"))
                .thenReturn(Optional.of(new UserRepository.UserCredential(1L, "User", "user@example.com", "hash", "active")));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(userRepository.findAuthoritiesByUserId(1L))
                .thenReturn(new UserRepository.UserAuthorities(Set.of("customer"), Set.of("ORDER_CREATE")));

        AuthResult result = useCase.execute(new LoginCommand("user@example.com", "secret"));

        assertThat(result.user().id()).isEqualTo(1L);
    }

    @Test
    void rejectsInvalidPasswordBeforeStatusCheck() {
        when(userRepository.findCredentialByEmail("user@example.com"))
                .thenReturn(Optional.of(new UserRepository.UserCredential(1L, "User", "user@example.com", "hash", "active")));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new LoginCommand("user@example.com", "bad")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
