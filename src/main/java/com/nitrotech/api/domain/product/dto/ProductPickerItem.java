package com.nitrotech.api.domain.product.dto;

import java.math.BigDecimal;

public record ProductPickerItem(
        Long id,
        String slug,
        String name,
        String categoryName,
        BigDecimal priceMin,
        BigDecimal priceMax,
        String thumbnail,
        String badge
) {}
