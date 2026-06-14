package com.nitrotech.api.domain.shipping.repository;

import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogSource;
import com.nitrotech.api.domain.shipping.dto.ShipmentStatus;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository {
    ShipmentData save(ShipmentData shipment);
    Optional<ShipmentData> findByOrderId(Long orderId);
    Optional<ShipmentData> findByProviderAndTrackingCode(String provider, String trackingCode);
    List<ShipmentLogData> findLogsByShipmentId(Long shipmentId);
    void addLog(Long shipmentId, ShipmentStatus status, String rawStatus, ShipmentLogSource source, String location, String note);
}
