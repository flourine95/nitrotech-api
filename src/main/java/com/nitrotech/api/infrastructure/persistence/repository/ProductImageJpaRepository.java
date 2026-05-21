package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.ProductImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageJpaRepository extends JpaRepository<ProductImageEntity, Long> {
    List<ProductImageEntity> findByProductIdOrderBySortOrderAsc(Long productId);
    List<ProductImageEntity> findByProductIdInOrderBySortOrderAsc(List<Long> productIds);
    void deleteByProductId(Long productId);
}
