package com.nitrotech.api.domain.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
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
        Integer weightGrams,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm,
        Integer stockQuantity,
        Integer lowStockThreshold,
        Boolean inStock,
        Boolean lowStock,
        Instant createdAt,
        Instant updatedAt
) {}
