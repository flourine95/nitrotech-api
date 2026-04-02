package com.nitrotech.api.domain.promotion.dto;

import java.math.BigDecimal;

public record ApplyPromotionResult(
        Long promotionId,
        String code,
        BigDecimal discountAmount,
        String description
) {}
