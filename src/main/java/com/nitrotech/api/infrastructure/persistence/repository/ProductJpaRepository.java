package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    /**
     * Batch count active variants for multiple products
     * Returns: [productId (Long), variantCount (Long)]
     */
    @Query("SELECT v.productId, COUNT(v) FROM ProductVariantEntity v WHERE v.productId IN :productIds AND v.deletedAt IS NULL AND v.active = true GROUP BY v.productId")
    List<Object[]> countActiveVariantsBatchRaw(@Param("productIds") List<Long> productIds);

    default Map<Long, Integer> countActiveVariantsBatch(List<Long> productIds) {
        return countActiveVariantsBatchRaw(productIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));
    }

    /**
     * Batch find minimum prices for multiple products
     * Returns: [productId (Long), minPrice (BigDecimal)]
     */
    @Query("SELECT v.productId, MIN(v.price) FROM ProductVariantEntity v WHERE v.productId IN :productIds AND v.deletedAt IS NULL AND v.active = true GROUP BY v.productId")
    List<Object[]> findMinPricesBatchRaw(@Param("productIds") List<Long> productIds);

    default Map<Long, BigDecimal> findMinPricesBatch(List<Long> productIds) {
        return findMinPricesBatchRaw(productIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1]
                ));
    }

    /**
     * Batch find maximum prices for multiple products
     * Returns: [productId (Long), maxPrice (BigDecimal)]
     */
    @Query("SELECT v.productId, MAX(v.price) FROM ProductVariantEntity v WHERE v.productId IN :productIds AND v.deletedAt IS NULL AND v.active = true GROUP BY v.productId")
    List<Object[]> findMaxPricesBatchRaw(@Param("productIds") List<Long> productIds);

    default Map<Long, BigDecimal> findMaxPricesBatch(List<Long> productIds) {
        return findMaxPricesBatchRaw(productIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1]
                ));
    }

    /**
     * Search products for picker (lightweight query)
     * Returns: [id, slug, name, categoryName, priceMin, priceMax, thumbnail, manualBadge]
     */
    @Query(value = """
        SELECT 
            p.id,
            p.slug,
            p.name,
            c.name as category_name,
            MIN(v.price) as price_min,
            MAX(v.price) as price_max,
            p.thumbnail,
            p.manual_badge
        FROM products p
        LEFT JOIN categories c ON p.category_id = c.id
        LEFT JOIN brands b ON p.brand_id = b.id
        LEFT JOIN product_variants v ON v.product_id = p.id 
            AND v.deleted_at IS NULL 
            AND v.active = true
        WHERE p.deleted_at IS NULL
        AND p.active = true
        AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:categorySlug IS NULL OR c.slug = :categorySlug)
        AND (:brandSlug IS NULL OR b.slug = :brandSlug)
        GROUP BY p.id, p.slug, p.name, c.name, p.thumbnail, p.manual_badge
        ORDER BY p.name ASC
        """, nativeQuery = true)
    List<Object[]> searchWithoutExclude(
            @Param("search") String search,
            @Param("categorySlug") String categorySlug,
            @Param("brandSlug") String brandSlug,
            Pageable pageable
    );

    @Query(value = """
        SELECT 
            p.id,
            p.slug,
            p.name,
            c.name as category_name,
            MIN(v.price) as price_min,
            MAX(v.price) as price_max,
            p.thumbnail,
            p.manual_badge
        FROM products p
        LEFT JOIN categories c ON p.category_id = c.id
        LEFT JOIN brands b ON p.brand_id = b.id
        LEFT JOIN product_variants v ON v.product_id = p.id 
            AND v.deleted_at IS NULL 
            AND v.active = true
        WHERE p.deleted_at IS NULL
        AND p.active = true
        AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:categorySlug IS NULL OR c.slug = :categorySlug)
        AND (:brandSlug IS NULL OR b.slug = :brandSlug)
        AND p.id NOT IN :excludeIds
        GROUP BY p.id, p.slug, p.name, c.name, p.thumbnail, p.manual_badge
        ORDER BY p.name ASC
        """, nativeQuery = true)
    List<Object[]> searchWithExclude(
            @Param("search") String search,
            @Param("categorySlug") String categorySlug,
            @Param("brandSlug") String brandSlug,
            @Param("excludeIds") List<Long> excludeIds,
            Pageable pageable
    );
}
