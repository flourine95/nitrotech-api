package com.nitrotech.api.domain.payment.dto;

import java.math.BigDecimal;

public record PaymentOrderData(
        Long orderId,
        BigDecimal amount,
        String description
) {}
