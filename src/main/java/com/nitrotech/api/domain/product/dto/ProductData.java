package com.nitrotech.api.domain.product.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ProductData(
        Long id,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        String name,
        String slug,
        String description,
        String thumbnail,
        Map<String, Object> specs,
        boolean active,
        List<String> images,
        List<ProductVariantData> variants,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
