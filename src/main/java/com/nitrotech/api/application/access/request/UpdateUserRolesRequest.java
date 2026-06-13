package com.nitrotech.api.application.access.request;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateUserRolesRequest(
        @NotNull Set<String> roleSlugs
) {}
