package com.nitrotech.api.domain.order.dto;

public record OrderListQuery(
        Long userId,
        String status,
        int page,
        int size
) {}
