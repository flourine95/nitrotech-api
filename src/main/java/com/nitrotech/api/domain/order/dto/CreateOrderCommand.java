package com.nitrotech.api.domain.order.dto;

public record CreateOrderCommand(
        Long userId,
        Long addressId,
        String paymentMethod,
        String promotionCode,
        String note
) {}
