package com.nitrotech.api.domain.promotion.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PromotionData(
        Long id,
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
        String status,
        int totalUsed,
        Instant createdAt,
        Instant updatedAt
) {}
