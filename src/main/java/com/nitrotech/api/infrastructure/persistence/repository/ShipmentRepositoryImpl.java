package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogSource;
import com.nitrotech.api.domain.shipping.dto.ShipmentStatus;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.infrastructure.persistence.entity.ShipmentEntity;
import com.nitrotech.api.infrastructure.persistence.entity.ShipmentLogEntity;
import com.nitrotech.api.infrastructure.persistence.mapper.ShipmentLogMapper;
import com.nitrotech.api.infrastructure.persistence.mapper.ShipmentMapper;
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
    private final ShipmentMapper shipmentMapper;
    private final ShipmentLogMapper shipmentLogMapper;

    @Override
    @Transactional
    public ShipmentData save(ShipmentData data) {
        ShipmentEntity entity;
        if (data.getId() != null) {
            entity = shipmentJpa.findById(data.getId())
                    .orElseThrow(() -> new NotFoundException("SHIPMENT_NOT_FOUND", 
                            "Shipment with ID " + data.getId() + " not found"));
            shipmentMapper.updateEntity(entity, data);
        } else {
            entity = shipmentMapper.toEntity(data);
        }

        ShipmentEntity saved = shipmentJpa.save(entity);
        return shipmentMapper.toData(saved);
    }

    @Override
    public Optional<ShipmentData> findByOrderId(Long orderId) {
        return shipmentJpa.findByOrderId(orderId).map(shipmentMapper::toData);
    }

    @Override
    public Optional<ShipmentData> findByProviderAndTrackingCode(String provider, String trackingCode) {
        return shipmentJpa.findByProviderIgnoreCaseAndTrackingCode(provider, trackingCode).map(shipmentMapper::toData);
    }

    @Override
    public List<ShipmentLogData> findLogsByShipmentId(Long shipmentId) {
        return logJpa.findByShipment_IdOrderByCreatedAtAsc(shipmentId).stream()
                .map(shipmentLogMapper::toData)
                .toList();
    }

    @Override
    @Transactional
    public void addLog(Long shipmentId, ShipmentStatus status, String rawStatus, ShipmentLogSource source, String location, String note) {
        ShipmentEntity shipment = shipmentJpa.findById(shipmentId)
                .orElseThrow(() -> new NotFoundException("SHIPMENT_NOT_FOUND", 
                        "Shipment with ID " + shipmentId + " not found"));

        ShipmentLogEntity log = new ShipmentLogEntity();
        log.setShipment(shipment);
        log.setStatus(status.value());
        log.setRawStatus(rawStatus);
        log.setSource(source.name());
        log.setLocation(location);
        log.setNote(note);

        shipment.addLog(log);
    }
}
