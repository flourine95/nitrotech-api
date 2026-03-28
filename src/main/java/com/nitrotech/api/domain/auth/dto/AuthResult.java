package com.nitrotech.api.domain.auth.dto;

public record AuthResult(
        String accessToken,
        String refreshToken,
        String tokenType,
        UserData user
) {
    public static AuthResult of(TokenPair tokens, UserData user) {
        return new AuthResult(tokens.accessToken(), tokens.refreshToken(), "Bearer", user);
    }

    public record UserData(Long id, String name, String email) {}
}
