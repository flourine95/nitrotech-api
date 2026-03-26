package com.nitrotech.api.domain.auth.usecase;

public interface TokenProvider {
    String generate(String subject);
    String extractSubject(String token);
    boolean isValid(String token);
}
