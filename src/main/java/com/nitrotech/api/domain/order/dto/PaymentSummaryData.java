package com.nitrotech.api.domain.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentSummaryData(
        String provider,
        String status,
        BigDecimal amount,
        Instant paidAt
) {}
