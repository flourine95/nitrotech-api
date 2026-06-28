package com.nitrotech.api.domain.cart.dto;

import java.math.BigDecimal;
import java.util.Map;

public record CartVariantData(
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
        CartProductData product
) {}
