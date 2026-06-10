package com.nitrotech.api.domain.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingResult {
    private String trackingCode;
    private BigDecimal fee;
    private Instant estimatedAt;
}
