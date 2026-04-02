package com.nitrotech.api.domain.order.dto;

import java.math.BigDecimal;
import java.util.List;

// Internal DTO dùng để truyền data từ use case xuống repository
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
        List<OrderItemData> items
) {}
