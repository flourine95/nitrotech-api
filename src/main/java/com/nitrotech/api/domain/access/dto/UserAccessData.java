package com.nitrotech.api.domain.access.dto;

import java.util.Set;

public record UserAccessData(
        Long id,
        String name,
        String email,
        String status,
        Set<String> roleSlugs,
        Set<String> permissionSlugs
) {}
