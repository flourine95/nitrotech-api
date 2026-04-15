package com.nitrotech.api.domain.order.dto;

import java.math.BigDecimal;

public record OrderItemData(
        Long id,
        Long variantId,
        String name,
        String sku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
