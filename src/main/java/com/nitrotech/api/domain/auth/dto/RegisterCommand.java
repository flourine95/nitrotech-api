package com.nitrotech.api.domain.auth.dto;

public record RegisterCommand(
        String name,
        String email,
        String password
) {}
