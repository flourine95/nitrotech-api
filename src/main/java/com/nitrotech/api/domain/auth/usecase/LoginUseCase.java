package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.LoginCommand;
import com.nitrotech.api.domain.auth.exception.AccountNotActiveException;
import com.nitrotech.api.domain.auth.exception.InvalidCredentialsException;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResult execute(LoginCommand command) {
        UserRepository.UserCredential credential = userRepository.findCredentialByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.password(), credential.hashedPassword())) {
            throw new InvalidCredentialsException();
        }

        if (!"active".equals(credential.status())) {
            throw new AccountNotActiveException(credential.status());
        }

        return AuthResult.ofUser(new AuthResult.UserData(credential.id(), credential.name(), credential.email()));
    }
}
