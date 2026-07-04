package com.nitrotech.api.domain.order.dto;

public record CreateOrderCommand(
        Long userId,
        Long addressId,
        ShippingAddressSnapshot shippingAddress,
        String paymentMethod,
        String promotionCode,
        String note,
        String idempotencyKey
) {}
