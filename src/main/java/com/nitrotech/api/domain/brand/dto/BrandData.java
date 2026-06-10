package com.nitrotech.api.domain.brand.dto;

import java.time.Instant;

public record BrandData(
        Long id,
        String name,
        String slug,
        String logo,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
