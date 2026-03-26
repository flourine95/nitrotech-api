package com.nitrotech.api.shared.config;

import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.domain.auth.usecase.LoginUseCase;
import com.nitrotech.api.domain.auth.usecase.PasswordEncoder;
import com.nitrotech.api.domain.auth.usecase.RegisterUseCase;
import com.nitrotech.api.domain.auth.usecase.TokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {

    @Bean
    public RegisterUseCase registerUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenProvider tokenProvider) {
        return new RegisterUseCase(userRepository, passwordEncoder, tokenProvider);
    }

    @Bean
    public LoginUseCase loginUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenProvider tokenProvider) {
        return new LoginUseCase(userRepository, passwordEncoder, tokenProvider);
    }
}
