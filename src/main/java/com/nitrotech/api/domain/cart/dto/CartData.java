package com.nitrotech.api.domain.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartData(
        Long id,
        Long userId,
        List<CartItemData> items,
        int totalItems,
        BigDecimal totalAmount
) {}
