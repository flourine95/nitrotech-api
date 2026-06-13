package com.nitrotech.api.application.access.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoleRequest(
        @NotBlank String name,
        String description,
        Boolean active
) {}
