package com.nitrotech.api.domain.shipping.repository;

import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogData;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository {
    ShipmentData save(ShipmentData shipment);
    Optional<ShipmentData> findByOrderId(Long orderId);
    Optional<ShipmentData> findByProviderAndTrackingCode(String provider, String trackingCode);
    List<ShipmentLogData> findLogsByShipmentId(Long shipmentId);
    void addLog(Long shipmentId, String status, String rawStatus, String source, String location, String note);
}
