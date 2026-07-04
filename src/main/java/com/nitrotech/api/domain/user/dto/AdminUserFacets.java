package com.nitrotech.api.domain.user.dto;

import java.math.BigDecimal;

public record AdminUserFacets(
        long total,
        long active,
        long inactive,
        long newUsers,
        long withOrders,
        long noOrders,
        long atRisk,
        BigDecimal totalSpent
) {}
