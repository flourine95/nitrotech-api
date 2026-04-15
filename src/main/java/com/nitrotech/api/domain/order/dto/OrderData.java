package com.nitrotech.api.domain.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderData(
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
