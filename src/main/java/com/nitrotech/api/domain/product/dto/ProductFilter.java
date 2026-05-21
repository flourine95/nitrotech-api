package com.nitrotech.api.domain.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductFilter(
        String search,
        Boolean active,
        Boolean deleted,
        List<Long> categoryIds,
        List<Long> brandIds,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}
