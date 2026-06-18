package com.nitrotech.api.domain.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderData(
        Long id,
        Long userId,
        String orderCode,
        ShippingAddressSnapshot shippingAddress,
        String status,
        String paymentMethod,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal finalAmount,
        String promotionCode,
        String note,
        List<OrderItemData> items,
        Instant createdAt,
        Instant updatedAt
) {
    // Overloaded constructor for backwards compatibility (e.g. in unit tests)
    public OrderData(
            Long id,
            Long userId,
            ShippingAddressSnapshot shippingAddress,
            String status,
            String paymentMethod,
            BigDecimal totalAmount,
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            BigDecimal finalAmount,
            String promotionCode,
            String note,
            List<OrderItemData> items,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, userId, "SO-" + String.format("%03d", id), shippingAddress, status, paymentMethod,
                totalAmount, discountAmount, shippingFee, finalAmount, promotionCode, note, items,
                createdAt, updatedAt);
    }
}
