package com.nitrotech.api.domain.brand.dto;

import java.time.LocalDateTime;

public record BrandData(
        Long id,
        String name,
        String slug,
        String logo,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
