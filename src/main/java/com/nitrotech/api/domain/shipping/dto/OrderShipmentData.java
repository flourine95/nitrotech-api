package com.nitrotech.api.domain.shipping.dto;

import java.util.List;

public record OrderShipmentData(
        ShipmentData shipment,
        List<ShipmentLogData> logs
) {}
