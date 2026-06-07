package com.nitrotech.api.domain.cart.dto;

import java.util.List;

public record CartData(
        Long id,
        Long userId,
        List<CartItemData> items,
        CartSummaryData summary
) {
}
