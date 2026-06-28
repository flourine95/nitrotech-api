package com.nitrotech.api.domain.shipping.dto;

import java.math.BigDecimal;

public record ShippingFeeQuote(
        BigDecimal fee,
        BigDecimal insuranceFee,
        boolean delivery
) {}
