package com.nitrotech.api.infrastructure.payment.sepay.dto;

import java.math.BigDecimal;

public record SepayWebhookPayload(
        Long id,
        String gateway,
        String transactionDate,
        String accountNumber,
        String subAccount,
        String code,
        String content,
        String transferType,
        String description,
        BigDecimal transferAmount,
        BigDecimal accumulated,
        String referenceCode
) {}
