package com.nitrotech.api.domain.product.dto;

import java.math.BigDecimal;
import java.util.Map;

public record CreateVariantCommand(
        String sku,
        String name,
        BigDecimal price,
        Map<String, Object> attributes,
        boolean active
) {}
