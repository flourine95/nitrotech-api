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
        Instant updatedAt,
        UserSummaryData user,
        PaymentSummaryData payment
) {}
