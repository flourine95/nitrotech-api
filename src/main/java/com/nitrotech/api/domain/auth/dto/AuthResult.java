package com.nitrotech.api.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResult(
        String accessToken,
        String refreshToken,
        String tokenType,
        UserData user
) {
    public static AuthResult of(TokenPair tokens, UserData user) {
        return new AuthResult(tokens.accessToken(), tokens.refreshToken(), "Bearer", user);
    }

    public static AuthResult ofUser(UserData user) {
        return new AuthResult(null, null, null, user);
    }

    public record UserData(Long id, String name, String email) {}
}
