package com.nitrotech.api.domain.promotion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status
) {}
