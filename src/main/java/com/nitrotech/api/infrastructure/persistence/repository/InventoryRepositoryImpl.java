package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.inventory.dto.InventoryData;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.infrastructure.persistence.entity.InventoryEntity;
import com.nitrotech.api.infrastructure.persistence.entity.ProductVariantEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class InventoryRepositoryImpl implements InventoryRepository {

    private final InventoryJpaRepository jpa;
    private final ProductVariantJpaRepository variantJpa;

    public InventoryRepositoryImpl(InventoryJpaRepository jpa, ProductVariantJpaRepository variantJpa) {
        this.jpa = jpa;
        this.variantJpa = variantJpa;
    }

    @Override
    @Transactional
    public InventoryData getOrCreate(Long variantId) {
        return toData(jpa.findByVariantId(variantId).orElseGet(() -> {
            InventoryEntity e = new InventoryEntity();
            e.setVariantId(variantId);
            return jpa.save(e);
        }));
    }

    @Override
    @Transactional
    public InventoryData adjust(Long variantId, int delta) {
        InventoryEntity entity = getOrCreateEntity(variantId);
        entity.setQuantity(entity.getQuantity() + delta);
        entity.setUpdatedAt(LocalDateTime.now());
        return toData(jpa.save(entity));
    }

    @Override
    @Transactional
    public InventoryData setQuantity(Long variantId, int quantity) {
        InventoryEntity entity = getOrCreateEntity(variantId);
        entity.setQuantity(quantity);
        entity.setUpdatedAt(LocalDateTime.now());
        return toData(jpa.save(entity));
    }

    @Override
    @Transactional
    public InventoryData setThreshold(Long variantId, int threshold) {
        InventoryEntity entity = getOrCreateEntity(variantId);
        entity.setLowStockThreshold(threshold);
        entity.setUpdatedAt(LocalDateTime.now());
        return toData(jpa.save(entity));
    }

    @Override
    public Optional<InventoryData> findByVariantId(Long variantId) {
        return jpa.findByVariantId(variantId).map(this::toData);
    }

    @Override
    public List<InventoryData> findLowStock() {
        return jpa.findLowStock().stream().map(this::toData).toList();
    }

    @Override
    public int getQuantity(Long variantId) {
        return jpa.findByVariantId(variantId).map(InventoryEntity::getQuantity).orElse(0);
    }

    @Override
    public boolean hasSufficientStock(Long variantId, int required) {
        return getQuantity(variantId) >= required;
    }

    private InventoryEntity getOrCreateEntity(Long variantId) {
        return jpa.findByVariantId(variantId).orElseGet(() -> {
            InventoryEntity e = new InventoryEntity();
            e.setVariantId(variantId);
            return jpa.save(e);
        });
    }

    private InventoryData toData(InventoryEntity e) {
        ProductVariantEntity variant = variantJpa.findById(e.getVariantId()).orElse(null);
        return new InventoryData(
                e.getId(), e.getVariantId(),
                variant != null ? variant.getSku() : null,
                variant != null ? variant.getName() : null,
                e.getQuantity(), e.getLowStockThreshold(),
                e.getQuantity() <= e.getLowStockThreshold(),
                e.getUpdatedAt()
        );
    }
}
