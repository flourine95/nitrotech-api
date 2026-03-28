package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.LoginCommand;
import com.nitrotech.api.domain.auth.dto.TokenPair;
import com.nitrotech.api.domain.auth.exception.AccountNotActiveException;
import com.nitrotech.api.domain.auth.exception.InvalidCredentialsException;
import com.nitrotech.api.domain.auth.repository.RefreshTokenRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public LoginUseCase(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder, TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
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

        String accessToken = tokenProvider.generate(credential.email());
        String refreshToken = refreshTokenRepository.create(credential.id(), 30);
        AuthResult.UserData user = new AuthResult.UserData(credential.id(), credential.name(), credential.email());

        return AuthResult.of(TokenPair.of(accessToken, refreshToken), user);
    }
}
