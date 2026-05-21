package com.nitrotech.api.domain.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductFilter(
        String search,
        Boolean active,
        Boolean deleted,
        Long categoryId,           // Deprecated: use categoryIds
        Long brandId,              // Deprecated: use brandIds
        List<Long> categoryIds,    // Multiple category filter
        List<Long> brandIds,       // Multiple brand filter
        BigDecimal minPrice,       // Minimum price filter
        BigDecimal maxPrice        // Maximum price filter
) {
}
