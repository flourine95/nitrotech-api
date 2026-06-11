package com.nitrotech.api.domain.payment.dto;

public record PaymentInitResult(
        String paymentUrl,
        boolean redirect
) {}
