package com.nitrotech.api.domain.auth.dto;

public record ChangePasswordCommand(
        Long userId,
        String currentPassword,
        String newPassword
) {}
