package com.nitrotech.api.domain.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record ProductVariantData(
        Long id,
        Long productId,
        String sku,
        String name,
        BigDecimal price,
        Map<String, Object> attributes,
        boolean active,
        Long imageId,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
