package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryJpaRepository extends JpaRepository<InventoryEntity, Long> {

    Optional<InventoryEntity> findByVariantId(Long variantId);

    List<InventoryEntity> findByVariantIdIn(List<Long> variantIds);

    @Modifying
    @Query(value = """
            UPDATE inventories
            SET quantity = quantity - :quantity,
                updated_at = NOW()
            WHERE variant_id = :variantId
              AND quantity >= :quantity
            """, nativeQuery = true)
    int deductIfEnough(@Param("variantId") Long variantId, @Param("quantity") int quantity);

    @Query("SELECT i FROM InventoryEntity i WHERE i.quantity <= i.lowStockThreshold ORDER BY i.quantity ASC")
    List<InventoryEntity> findLowStock();
}
