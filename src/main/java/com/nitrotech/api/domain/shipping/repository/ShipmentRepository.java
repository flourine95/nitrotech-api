package com.nitrotech.api.domain.shipping.repository;

import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogSource;
import com.nitrotech.api.domain.shipping.ShipmentStatus;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface ShipmentRepository {
    ShipmentData save(ShipmentData shipment);
    Optional<ShipmentData> findById(Long id);
    Optional<ShipmentData> findByOrderId(Long orderId);
    Optional<ShipmentData> findByProviderAndTrackingCode(String provider, String trackingCode);
    List<ShipmentLogData> findLogsByShipmentId(Long shipmentId);
    void addLog(Long shipmentId, ShipmentStatus status, String rawStatus, ShipmentLogSource source, String location, String note);
    boolean recordWebhookEvent(Long shipmentId, ShipmentStatus status, String rawStatus, Instant occurredAt,
                               String reasonCode, String note, String rawPayload, String eventKey);
}
