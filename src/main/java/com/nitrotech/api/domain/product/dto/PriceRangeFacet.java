package com.nitrotech.api.domain.product.dto;

import java.math.BigDecimal;

public record PriceRangeFacet(
        BigDecimal min,
        BigDecimal max,
        Integer count
) {}
