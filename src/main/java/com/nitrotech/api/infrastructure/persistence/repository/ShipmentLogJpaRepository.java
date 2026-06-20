package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.ShipmentLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ShipmentLogJpaRepository extends JpaRepository<ShipmentLogEntity, Long> {
    @Query("""
            select log from ShipmentLogEntity log
            where log.shipment.id = :shipmentId
            order by coalesce(log.occurredAt, log.createdAt) asc, log.createdAt asc
            """)
    List<ShipmentLogEntity> findByShipmentIdOrderByTimelineAsc(Long shipmentId);
}
