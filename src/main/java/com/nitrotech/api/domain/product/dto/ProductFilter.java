package com.nitrotech.api.domain.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductFilter(
        String search,
        Boolean active,
        Boolean deleted,
        String categorySlug,
        List<String> brandSlugs,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String badge
) {}
