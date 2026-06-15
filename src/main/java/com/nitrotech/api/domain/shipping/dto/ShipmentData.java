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
public class ShipmentData {
    private Long id;
    private Long orderId;
    private String provider;
    private String trackingCode;
    private ShipmentStatus status;
    private BigDecimal fee;
    private Instant estimatedAt;
    private Instant shippedAt;
    private Instant deliveredAt;
    private Instant createdAt;
    private Instant updatedAt;
}
