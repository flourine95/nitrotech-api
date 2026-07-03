package com.nitrotech.api.domain.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlaceOrderData(
        Long userId,
        ShippingAddressSnapshot shippingAddress,
        String paymentMethod,
        String promotionCode,
        String note,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal finalAmount,
        List<OrderItemData> items,
        String idempotencyKey
) {}
