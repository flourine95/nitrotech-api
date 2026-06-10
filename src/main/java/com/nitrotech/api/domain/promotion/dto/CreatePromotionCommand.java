package com.nitrotech.api.domain.promotion.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CreatePromotionCommand(
        String name,
        String description,
        String code,
        String type,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        boolean stackable,
        int priority,
        Integer usageLimit,
        int usagePerUser,
        Instant startAt,
        Instant endAt,
        String status
) {}
