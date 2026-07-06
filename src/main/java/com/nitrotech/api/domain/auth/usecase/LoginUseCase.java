package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.UserStatus;
import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.LoginCommand;
import com.nitrotech.api.domain.auth.exception.AccountNotActiveException;
import com.nitrotech.api.domain.auth.exception.InvalidCredentialsException;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResult execute(LoginCommand command) {
        UserRepository.UserCredential credential = userRepository.findCredentialByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.password(), credential.hashedPassword())) {
            throw new InvalidCredentialsException();
        }

        if (UserStatus.fromValue(credential.status()) != UserStatus.active) {
            throw new AccountNotActiveException(credential.status());
        }

        UserRepository.UserAuthorities authorities = userRepository.findAuthoritiesByUserId(credential.id());
        return AuthResult.ofUser(new AuthResult.UserData(
                credential.id(),
                credential.name(),
                credential.email(),
                authorities.roles(),
                authorities.permissions()
        ));
    }
}
