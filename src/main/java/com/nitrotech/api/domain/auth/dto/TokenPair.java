package com.nitrotech.api.domain.auth.dto;

public record TokenPair(
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public static TokenPair of(String accessToken, String refreshToken) {
        return new TokenPair(accessToken, refreshToken, "Bearer");
    }
}
