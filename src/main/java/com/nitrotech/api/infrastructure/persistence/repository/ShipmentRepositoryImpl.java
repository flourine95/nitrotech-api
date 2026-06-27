package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.shipping.exception.ShipmentNotFoundException;

import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogSource;
import com.nitrotech.api.domain.shipping.ShipmentStatus;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.infrastructure.persistence.entity.ShipmentEntity;
import com.nitrotech.api.infrastructure.persistence.entity.ShipmentLogEntity;
import com.nitrotech.api.infrastructure.persistence.mapper.ShipmentLogMapper;
import com.nitrotech.api.infrastructure.persistence.mapper.ShipmentMapper;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.sql.Timestamp;

@Repository
@RequiredArgsConstructor
public class ShipmentRepositoryImpl implements ShipmentRepository {

    private final ShipmentJpaRepository shipmentJpa;
    private final ShipmentLogJpaRepository logJpa;
    private final ShipmentMapper shipmentMapper;
    private final ShipmentLogMapper shipmentLogMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public ShipmentData save(ShipmentData data) {
        ShipmentEntity entity;
        if (data.getId() != null) {
            entity = shipmentJpa.findById(data.getId())
                    .orElseThrow(() -> ShipmentNotFoundException.withId(data.getId()));
            shipmentMapper.updateEntity(entity, data);
        } else {
            entity = shipmentMapper.toEntity(data);
        }

        ShipmentEntity saved = shipmentJpa.save(entity);
        return shipmentMapper.toData(saved);
    }

    @Override
    public Optional<ShipmentData> findById(Long id) {
        return shipmentJpa.findById(id).map(shipmentMapper::toData);
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
        return logJpa.findByShipmentIdOrderByTimelineAsc(shipmentId).stream()
                .map(shipmentLogMapper::toData)
                .toList();
    }

    @Override
    @Transactional
    public void addLog(Long shipmentId, ShipmentStatus status, String rawStatus, ShipmentLogSource source, String location, String note) {
        ShipmentEntity shipment = shipmentJpa.findById(shipmentId)
                .orElseThrow(() -> ShipmentNotFoundException.withId(shipmentId));

        ShipmentLogEntity log = new ShipmentLogEntity();
        log.setShipment(shipment);
        log.setStatus(status.value());
        log.setRawStatus(rawStatus);
        log.setSource(source.name());
        log.setLocation(location);
        log.setNote(note);

        shipment.addLog(log);
    }

    @Override
    @Transactional
    public boolean recordWebhookEvent(Long shipmentId, ShipmentStatus status, String rawStatus, Instant occurredAt,
                                      String reasonCode, String note, String rawPayload, String eventKey) {
        return jdbcTemplate.update("""
                insert into shipment_logs (
                    shipment_id, status, raw_status, source, location, note,
                    occurred_at, reason_code, raw_payload, event_key
                ) values (?, ?, ?, 'WEBHOOK', null, ?, ?, ?, cast(? as jsonb), ?)
                on conflict (event_key) where event_key is not null do nothing
                """,
                shipmentId,
                status.value(),
                rawStatus,
                note,
                occurredAt == null ? null : Timestamp.from(occurredAt),
                reasonCode,
                rawPayload,
                eventKey
        ) == 1;
    }
}
