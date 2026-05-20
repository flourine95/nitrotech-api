package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long>,
        JpaSpecificationExecutor<ProductEntity> {

    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);

    Optional<ProductEntity> findBySlugAndDeletedAtIsNull(String slug);

    @Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL AND p.id = :id")
    Optional<ProductEntity> findActiveById(@Param("id") Long id);

    @Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NOT NULL AND p.id = :id")
    Optional<ProductEntity> findDeletedById(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END FROM ProductEntity p WHERE p.id = :id AND p.deletedAt IS NULL")
    boolean existsActiveById(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END FROM ProductEntity p WHERE p.categoryId = :categoryId AND p.deletedAt IS NULL")
    boolean existsAnyByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END FROM ProductEntity p WHERE p.brandId = :brandId AND p.deletedAt IS NULL")
    boolean existsAnyByBrandId(@Param("brandId") Long brandId);

    @Query("SELECT DISTINCT p.brandId FROM ProductEntity p WHERE p.brandId IN :brandIds AND p.deletedAt IS NULL")
    List<Long> findBrandIdsWithProducts(@Param("brandIds") List<Long> brandIds);

    @Query("SELECT COUNT(v) FROM ProductVariantEntity v WHERE v.productId = :productId AND v.deletedAt IS NULL AND v.active = true")
    int countActiveVariants(@Param("productId") Long productId);

    @Query("SELECT MIN(v.price) FROM ProductVariantEntity v WHERE v.productId = :productId AND v.deletedAt IS NULL AND v.active = true")
    BigDecimal findMinPrice(@Param("productId") Long productId);

    @Query("SELECT MAX(v.price) FROM ProductVariantEntity v WHERE v.productId = :productId AND v.deletedAt IS NULL AND v.active = true")
    BigDecimal findMaxPrice(@Param("productId") Long productId);

    // ── Batch queries ─────────────────────────────────────────────────────────

    @Query("SELECT v.productId, COUNT(v) FROM ProductVariantEntity v WHERE v.productId IN :productIds AND v.deletedAt IS NULL AND v.active = true GROUP BY v.productId")
    List<Object[]> countActiveVariantsBatchRaw(@Param("productIds") List<Long> productIds);

    default java.util.Map<Long, Integer> countActiveVariantsBatch(List<Long> productIds) {
        return countActiveVariantsBatchRaw(productIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));
    }

    @Query("SELECT v.productId, MIN(v.price) FROM ProductVariantEntity v WHERE v.productId IN :productIds AND v.deletedAt IS NULL AND v.active = true GROUP BY v.productId")
    List<Object[]> findMinPricesBatchRaw(@Param("productIds") List<Long> productIds);

    default java.util.Map<Long, BigDecimal> findMinPricesBatch(List<Long> productIds) {
        return findMinPricesBatchRaw(productIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1]
                ));
    }

    @Query("SELECT v.productId, MAX(v.price) FROM ProductVariantEntity v WHERE v.productId IN :productIds AND v.deletedAt IS NULL AND v.active = true GROUP BY v.productId")
    List<Object[]> findMaxPricesBatchRaw(@Param("productIds") List<Long> productIds);

    default java.util.Map<Long, BigDecimal> findMaxPricesBatch(List<Long> productIds) {
        return findMaxPricesBatchRaw(productIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1]
                ));
    }
}
