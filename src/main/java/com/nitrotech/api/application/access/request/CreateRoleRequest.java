package com.nitrotech.api.application.access.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateRoleRequest(
        @NotBlank String name,
        @NotBlank
        @Pattern(regexp = "^[a-z0-9_]+$", message = "Role slug must use lowercase letters, numbers, and underscores")
        String slug,
        String description
) {}
