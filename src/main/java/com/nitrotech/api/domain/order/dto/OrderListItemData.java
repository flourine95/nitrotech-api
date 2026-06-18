package com.nitrotech.api.domain.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderListItemData(
        Long id,
        Long userId,
        String orderCode,
        String receiver,
        String phone,
        String email,
        String status,
        String paymentMethod,
        String paymentStatus,
        Boolean hasShipment,
        String shipmentStatus,
        String trackingCode,
        BigDecimal finalAmount,
        Long itemCount,
        Instant createdAt,
        Instant updatedAt
) {
    public OrderListItemData(
            Long id,
            Long userId,
            String orderCode,
            Object receiver,
            Object phone,
            Object email,
            String status,
            String paymentMethod,
            String paymentStatus,
            Boolean hasShipment,
            String shipmentStatus,
            String trackingCode,
            BigDecimal finalAmount,
            Long itemCount,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, userId, orderCode, stringValue(receiver), stringValue(phone), stringValue(email), status, paymentMethod,
                paymentStatus, hasShipment, shipmentStatus, trackingCode, finalAmount, itemCount, createdAt, updatedAt);
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
