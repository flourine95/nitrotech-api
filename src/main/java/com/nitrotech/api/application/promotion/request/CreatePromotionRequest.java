package com.nitrotech.api.application.promotion.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreatePromotionRequest(
        @NotBlank String name,
        String description,

        @Pattern(regexp = "^[A-Z0-9_-]{3,50}$", message = "Code must be 3-50 uppercase letters, numbers, hyphens or underscores")
        String code,

        @NotBlank @Pattern(regexp = "^(percentage|fixed|freeship)$", message = "Type must be percentage, fixed or freeship")
        String type,

        @NotNull @DecimalMin("0.01")
        BigDecimal discountValue,

        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        boolean stackable,
        int priority,
        Integer usageLimit,

        @Min(1)
        int usagePerUser,

        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,

        @Pattern(regexp = "^(draft|active|paused)$")
        String status
) {}
