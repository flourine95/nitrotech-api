package com.nitrotech.api.domain.auth.dto;

public record OAuthUserInfo(
        String externalId,
        String email,
        String name,
        String avatar,
        boolean emailVerified
) {
}
