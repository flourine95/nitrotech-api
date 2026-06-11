package com.nitrotech.api.domain.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record VerifiedPaymentWebhook(
        String provider,
        String externalTransactionId,
        Long orderId,
        BigDecimal amount,
        String status,
        Instant paidAt,
        String rawContent,
        Map<String, Object> rawData
) {}
