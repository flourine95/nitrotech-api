package com.nitrotech.api.domain.shipping.dto;

import java.time.Instant;

public record ShipmentLogData(
        Long id,
        Long shipmentId,
        ShipmentStatus status,
        String rawStatus,
        String source,
        String location,
        String note,
        Instant createdAt
) {}
