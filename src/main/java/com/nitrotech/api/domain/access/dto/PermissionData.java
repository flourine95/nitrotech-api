package com.nitrotech.api.domain.access.dto;

public record PermissionData(
        Long id,
        String name,
        String slug,
        String groupName,
        String description,
        boolean systemPermission
) {}
