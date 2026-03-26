package com.nitrotech.api.domain.auth.dto;

public record AuthResult(
        String accessToken,
        String tokenType,
        UserData user
) {
    public static AuthResult of(String token, UserData user) {
        return new AuthResult(token, "Bearer", user);
    }

    public record UserData(Long id, String name, String email) {}
}
