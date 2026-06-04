package com.nitrotech.api.domain.cart.dto;

import java.math.BigDecimal;

public record CartSummaryData(
        int totalItems,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal shipping,
        BigDecimal total
) {}
