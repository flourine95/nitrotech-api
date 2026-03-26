package com.nitrotech.api.domain.auth.usecase;

public interface PasswordEncoder {
    String encode(String raw);
    boolean matches(String raw, String encoded);
}
