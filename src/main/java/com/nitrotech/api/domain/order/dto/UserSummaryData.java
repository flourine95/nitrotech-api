package com.nitrotech.api.domain.order.dto;

public record UserSummaryData(
        String name,
        String email,
        String phone,
        String avatar
) {}
