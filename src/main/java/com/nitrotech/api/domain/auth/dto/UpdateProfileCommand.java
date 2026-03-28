package com.nitrotech.api.domain.auth.dto;

public record UpdateProfileCommand(
        Long userId,
        String name,
        String phone,
        String avatar
) {}
