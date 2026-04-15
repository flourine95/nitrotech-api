package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryJpaRepository extends JpaRepository<InventoryEntity, Long> {

    Optional<InventoryEntity> findByVariantId(Long variantId);

    @Query("SELECT i FROM InventoryEntity i WHERE i.quantity <= i.lowStockThreshold ORDER BY i.quantity ASC")
    List<InventoryEntity> findLowStock();
}
