package com.nitrotech.api.domain.auth.dto;

import java.util.Set;

public record UserProfileData(
        Long id,
        String name,
        String email,
        String phone,
        String avatar,
        String status,
        String provider,
        Set<String> roles,
        Set<String> permissions
) {
    public UserProfileData(
            Long id,
            String name,
            String email,
            String phone,
            String avatar,
            String status,
            String provider
    ) {
        this(id, name, email, phone, avatar, status, provider, Set.of(), Set.of());
    }
}
