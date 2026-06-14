package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.ShipmentLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentLogJpaRepository extends JpaRepository<ShipmentLogEntity, Long> {
    List<ShipmentLogEntity> findByShipment_IdOrderByCreatedAtAsc(Long shipmentId);
}
