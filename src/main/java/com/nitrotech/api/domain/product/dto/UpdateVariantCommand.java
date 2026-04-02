package com.nitrotech.api.domain.product.dto;

import java.math.BigDecimal;
import java.util.Map;

public record UpdateVariantCommand(
        Long id,
        Long productId,
        String sku,
        String name,
        BigDecimal price,
        Map<String, Object> attributes,
        Boolean active
) {}
