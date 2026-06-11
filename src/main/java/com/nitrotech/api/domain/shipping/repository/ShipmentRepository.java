package com.nitrotech.api.domain.shipping.repository;

import com.nitrotech.api.domain.shipping.dto.ShipmentData;

import java.util.Optional;

public interface ShipmentRepository {
    ShipmentData save(ShipmentData shipment);
    Optional<ShipmentData> findByOrderId(Long orderId);
    Optional<ShipmentData> findByProviderAndTrackingCode(String provider, String trackingCode);
    void addLog(Long shipmentId, String status, String location, String note);
}
