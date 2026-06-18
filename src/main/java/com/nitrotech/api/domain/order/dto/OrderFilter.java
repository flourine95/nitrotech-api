package com.nitrotech.api.domain.order.dto;

import java.time.Instant;
import java.math.BigDecimal;

public record OrderFilter(
        Long userId,
        String search,
        String status,
        String paymentMethod,
        Instant createdFrom,
        Instant createdToExclusive,
        BigDecimal amountMin,
        BigDecimal amountMax
) {}
