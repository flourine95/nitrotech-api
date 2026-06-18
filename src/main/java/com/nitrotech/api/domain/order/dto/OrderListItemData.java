package com.nitrotech.api.domain.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderListItemData(
        Long id,
        Long userId,
        String receiver,
        String phone,
        String status,
        String paymentMethod,
        BigDecimal finalAmount,
        Long itemCount,
        Instant createdAt,
        Instant updatedAt
) {
    public OrderListItemData(
            Long id,
            Long userId,
            Object receiver,
            Object phone,
            String status,
            String paymentMethod,
            BigDecimal finalAmount,
            Long itemCount,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, userId, stringValue(receiver), stringValue(phone), status, paymentMethod,
                finalAmount, itemCount, createdAt, updatedAt);
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
