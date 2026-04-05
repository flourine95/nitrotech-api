package com.nitrotech.api.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenPair(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn
) {
    public static TokenPair of(String accessToken, String refreshToken) {
        return new TokenPair(accessToken, refreshToken, "Bearer", null);
    }

    public static TokenPair of(String accessToken, String refreshToken, long expiresInMs) {
        return new TokenPair(accessToken, refreshToken, "Bearer", expiresInMs / 1000);
    }

    public static TokenPair ofSeconds(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenPair(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
