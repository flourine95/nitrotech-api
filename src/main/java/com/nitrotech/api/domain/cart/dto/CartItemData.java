package com.nitrotech.api.domain.cart.dto;

import java.math.BigDecimal;

public record CartItemData(
        Long id,
        Long variantId,
        String variantSku,
        String variantName,
        BigDecimal variantPrice,
        String productName,
        String productThumbnail,
        int quantity,
        BigDecimal subtotal
) {}
