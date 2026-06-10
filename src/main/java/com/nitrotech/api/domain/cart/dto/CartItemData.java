package com.nitrotech.api.domain.cart.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CartItemData(
        Long id,
        Long cartId,
        Long variantId,
        CartVariantData variant,
        int quantity,
        BigDecimal subtotal,
        Instant createdAt,
        Instant updatedAt
) {}
