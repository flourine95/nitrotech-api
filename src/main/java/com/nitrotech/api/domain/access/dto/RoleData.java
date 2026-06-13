package com.nitrotech.api.domain.access.dto;

import java.util.Set;

public record RoleData(
        Long id,
        String name,
        String slug,
        String description,
        boolean active,
        boolean systemRole,
        Set<String> permissionSlugs
) {}
