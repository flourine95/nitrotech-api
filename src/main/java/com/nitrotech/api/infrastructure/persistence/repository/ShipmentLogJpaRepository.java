package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.ShipmentLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentLogJpaRepository extends JpaRepository<ShipmentLogEntity, Long> {
}
