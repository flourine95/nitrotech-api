package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantJpaRepository extends JpaRepository<ProductVariantEntity, Long> {

    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);

    @Query("SELECT v FROM ProductVariantEntity v WHERE v.productId = :productId AND v.deletedAt IS NULL ORDER BY v.createdAt ASC")
    List<ProductVariantEntity> findActiveByProductId(@Param("productId") Long productId);

    @Query("SELECT v FROM ProductVariantEntity v WHERE v.id = :id AND v.deletedAt IS NULL")
    Optional<ProductVariantEntity> findActiveById(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN TRUE ELSE FALSE END FROM ProductVariantEntity v WHERE v.id = :id AND v.deletedAt IS NULL")
    boolean existsActiveById(@Param("id") Long id);
}
