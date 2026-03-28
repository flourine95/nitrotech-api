package com.nitrotech.api.domain.auth.dto;

public record UserProfileData(
        Long id,
        String name,
        String email,
        String phone,
        String avatar,
        String status,
        String provider
) {}
