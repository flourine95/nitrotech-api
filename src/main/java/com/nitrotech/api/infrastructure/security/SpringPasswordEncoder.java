package com.nitrotech.api.infrastructure.security;

import com.nitrotech.api.domain.auth.usecase.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SpringPasswordEncoder implements PasswordEncoder {

    private final org.springframework.security.crypto.password.PasswordEncoder delegate;

    public SpringPasswordEncoder(org.springframework.security.crypto.password.PasswordEncoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public String encode(String raw) {
        return delegate.encode(raw);
    }

    @Override
    public boolean matches(String raw, String encoded) {
        return delegate.matches(raw, encoded);
    }
}
