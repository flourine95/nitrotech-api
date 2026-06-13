package com.nitrotech.api.application.access.request;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateRolePermissionsRequest(
        @NotNull Set<String> permissionSlugs
) {}
