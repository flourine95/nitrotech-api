package com.nitrotech.api.domain.cart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CartItemData(
        Long id,
        Long cartId,
        Long variantId,
        CartVariantData variant,
        int quantity,
        BigDecimal subtotal,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
