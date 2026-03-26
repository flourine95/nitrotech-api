package com.nitrotech.api.domain.auth.dto;

public record LoginCommand(
        String email,
        String password
) {}
