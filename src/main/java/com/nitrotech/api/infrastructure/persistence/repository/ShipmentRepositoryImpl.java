package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogData;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.infrastructure.persistence.entity.ShipmentEntity;
import com.nitrotech.api.infrastructure.persistence.entity.ShipmentLogEntity;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ShipmentRepositoryImpl implements ShipmentRepository {

    private final ShipmentJpaRepository shipmentJpa;
    private final ShipmentLogJpaRepository logJpa;

    @Override
    @Transactional
    public ShipmentData save(ShipmentData data) {
        ShipmentEntity entity;
        if (data.getId() != null) {
            entity = shipmentJpa.findById(data.getId())
                    .orElseThrow(() -> new NotFoundException("SHIPMENT_NOT_FOUND", 
                            "Shipment with ID " + data.getId() + " not found"));
        } else {
            entity = new ShipmentEntity();
        }

        entity.setOrderId(data.getOrderId());
        entity.setProvider(data.getProvider());
        entity.setTrackingCode(data.getTrackingCode());
        entity.setStatus(data.getStatus());
        entity.setFee(data.getFee());
        entity.setEstimatedAt(data.getEstimatedAt());
        entity.setShippedAt(data.getShippedAt());
        entity.setDeliveredAt(data.getDeliveredAt());

        ShipmentEntity saved = shipmentJpa.save(entity);
        return toData(saved);
    }

    @Override
    public Optional<ShipmentData> findByOrderId(Long orderId) {
        return shipmentJpa.findByOrderId(orderId).map(this::toData);
    }

    @Override
    public Optional<ShipmentData> findByProviderAndTrackingCode(String provider, String trackingCode) {
        return shipmentJpa.findByProviderIgnoreCaseAndTrackingCode(provider, trackingCode).map(this::toData);
    }

    @Override
    public List<ShipmentLogData> findLogsByShipmentId(Long shipmentId) {
        return logJpa.findByShipment_IdOrderByCreatedAtAsc(shipmentId).stream()
                .map(this::toLogData)
                .toList();
    }

    @Override
    @Transactional
    public void addLog(Long shipmentId, String status, String rawStatus, String source, String location, String note) {
        ShipmentEntity shipment = shipmentJpa.findById(shipmentId)
                .orElseThrow(() -> new NotFoundException("SHIPMENT_NOT_FOUND", 
                        "Shipment with ID " + shipmentId + " not found"));

        ShipmentLogEntity log = new ShipmentLogEntity();
        log.setShipment(shipment);
        log.setStatus(status);
        log.setRawStatus(rawStatus);
        log.setSource(source);
        log.setLocation(location);
        log.setNote(note);

        logJpa.save(log);
    }

    private ShipmentData toData(ShipmentEntity entity) {
        return ShipmentData.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .provider(entity.getProvider())
                .trackingCode(entity.getTrackingCode())
                .status(entity.getStatus())
                .fee(entity.getFee())
                .estimatedAt(entity.getEstimatedAt())
                .shippedAt(entity.getShippedAt())
                .deliveredAt(entity.getDeliveredAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ShipmentLogData toLogData(ShipmentLogEntity entity) {
        return new ShipmentLogData(
                entity.getId(),
                entity.getShipment().getId(),
                entity.getStatus(),
                entity.getRawStatus(),
                entity.getSource(),
                entity.getLocation(),
                entity.getNote(),
                entity.getCreatedAt()
        );
    }
}
