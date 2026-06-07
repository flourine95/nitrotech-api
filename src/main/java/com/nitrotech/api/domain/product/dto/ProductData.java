package com.nitrotech.api.domain.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ProductData(
        Long id,
        Long categoryId,
        String categoryName,
        String categorySlug,
        Long brandId,
        String brandName,
        String name,
        String slug,
        String description,
        String shortDescription,
        String thumbnail,
        Map<String, Object> specs,
        boolean active,
        List<String> images,
        List<ProductVariantData> variants,  // null trong list endpoint, đầy đủ trong detail
        Integer variantCount,               // số variant active
        BigDecimal priceMin,                // giá thấp nhất từ variants active
        BigDecimal priceMax,                // giá cao nhất từ variants active
        String badge,                       // "new", "bestseller", "lowstock", "preorder", null
        Double rating,                      // avg rating from reviews
        Integer reviewCount,                // total approved reviews
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
