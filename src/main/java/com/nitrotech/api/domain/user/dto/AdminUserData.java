package com.nitrotech.api.domain.user.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record AdminUserData(
        Long id,
        String name,
        String email,
        String phone,
        String avatar,
        String status,
        String provider,
        Set<String> roleSlugs,
        String customerState,
        long orderCount,
        BigDecimal totalSpent,
        BigDecimal averageOrderValue,
        Instant lastOrderAt,
        Instant createdAt,
        Instant updatedAt
) {}
