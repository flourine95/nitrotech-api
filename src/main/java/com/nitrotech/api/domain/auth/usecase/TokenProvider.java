package com.nitrotech.api.domain.auth.usecase;

import java.util.Date;

public interface TokenProvider {
    String generate(String subject);
    String extractSubject(String token);
    boolean isValid(String token);
    Date getExpiration(String token);
    long getExpirationMs();
}
