package com.nitrotech.api.domain.auth.dto;

public record OAuthTokenResponse(
        String accessToken,
        String tokenType,
        String scope
) {
}
