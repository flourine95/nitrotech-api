package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.RegisterCommand;
import com.nitrotech.api.domain.auth.dto.TokenPair;
import com.nitrotech.api.domain.auth.exception.EmailAlreadyExistsException;
import com.nitrotech.api.domain.auth.repository.RefreshTokenRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public RegisterUseCase(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder, TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthResult execute(RegisterCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException(command.email());
        }

        String hashed = passwordEncoder.encode(command.password());
        AuthResult.UserData user = userRepository.save(command.name(), command.email(), hashed);

        String accessToken = tokenProvider.generate(user.email());
        String refreshToken = refreshTokenRepository.create(user.id(), 30);

        return AuthResult.of(TokenPair.of(accessToken, refreshToken), user);
    }
}
