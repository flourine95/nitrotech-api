package com.nitrotech.api.domain.product.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record UpdateProductCommand(
        Long id,
        Long categoryId,
        Long brandId,
        String name,
        String slug,
        String description,
        String shortDescription,
        String thumbnail,
        Map<String, Object> specs,
        Boolean active,
        List<String> images,
        String manualBadge,
        Instant manualBadgeExpiresAt
) {}
