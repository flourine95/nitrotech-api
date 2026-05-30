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

    // JOIN FETCH for single product with relationships
    @Query("SELECT p FROM ProductEntity p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<ProductEntity> findActiveByIdWithRelations(@Param("id") Long id);

    @Query("SELECT p FROM ProductEntity p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE p.slug = :slug AND p.deletedAt IS NULL")
    Optional<ProductEntity> findBySlugWithRelations(@Param("slug") String slug);

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

    @Query(value = """
        SELECT 
            c.id,
            c.name,
            c.slug,
            COUNT(DISTINCT p.id) as product_count
        FROM categories c
        INNER JOIN products p ON p.category_id = c.id
        LEFT JOIN brands b ON p.brand_id = b.id
        LEFT JOIN product_variants v ON v.product_id = p.id 
            AND v.deleted_at IS NULL 
            AND v.active = true
        WHERE c.deleted_at IS NULL
        AND p.deleted_at IS NULL
        AND p.active = true
        AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:minPrice IS NULL OR v.price >= :minPrice)
        AND (:maxPrice IS NULL OR v.price < :maxPrice)
        GROUP BY c.id, c.name, c.slug
        HAVING COUNT(DISTINCT p.id) > 0
        ORDER BY c.name ASC
        """, nativeQuery = true)
    List<Object[]> findCategoryFacetsWithoutBrands(
            @Param("search") String search,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    @Query(value = """
        SELECT 
            c.id,
            c.name,
            c.slug,
            COUNT(DISTINCT p.id) as product_count
        FROM categories c
        INNER JOIN products p ON p.category_id = c.id
        INNER JOIN brands b ON p.brand_id = b.id
        LEFT JOIN product_variants v ON v.product_id = p.id 
            AND v.deleted_at IS NULL 
            AND v.active = true
        WHERE c.deleted_at IS NULL
        AND p.deleted_at IS NULL
        AND p.active = true
        AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND b.slug IN :brandSlugs
        AND (:minPrice IS NULL OR v.price >= :minPrice)
        AND (:maxPrice IS NULL OR v.price < :maxPrice)
        GROUP BY c.id, c.name, c.slug
        HAVING COUNT(DISTINCT p.id) > 0
        ORDER BY c.name ASC
        """, nativeQuery = true)
    List<Object[]> findCategoryFacetsWithBrands(
            @Param("search") String search,
            @Param("brandSlugs") List<String> brandSlugs,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    @Query(value = """
        SELECT 
            b.id,
            b.name,
            b.slug,
            COUNT(DISTINCT p.id) as product_count
        FROM brands b
        INNER JOIN products p ON p.brand_id = b.id
        LEFT JOIN categories c ON p.category_id = c.id
        LEFT JOIN product_variants v ON v.product_id = p.id 
            AND v.deleted_at IS NULL 
            AND v.active = true
        WHERE b.deleted_at IS NULL
        AND p.deleted_at IS NULL
        AND p.active = true
        AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:categorySlug IS NULL OR c.slug = :categorySlug)
        AND (:minPrice IS NULL OR v.price >= :minPrice)
        AND (:maxPrice IS NULL OR v.price < :maxPrice)
        GROUP BY b.id, b.name, b.slug
        HAVING COUNT(DISTINCT p.id) > 0
        ORDER BY b.name ASC
        """, nativeQuery = true)
    List<Object[]> findBrandFacets(
            @Param("search") String search,
            @Param("categorySlug") String categorySlug,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    @Query(value = """
        SELECT 
            MIN(v.price) as min_price,
            MAX(v.price) as max_price
        FROM products p
        LEFT JOIN categories c ON p.category_id = c.id
        LEFT JOIN brands b ON p.brand_id = b.id
        INNER JOIN product_variants v ON v.product_id = p.id 
            AND v.deleted_at IS NULL 
            AND v.active = true
        WHERE p.deleted_at IS NULL
        AND p.active = true
        AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:categorySlug IS NULL OR c.slug = :categorySlug)
        """, nativeQuery = true)
    List<Object[]> findPriceRangeWithoutBrands(
            @Param("search") String search,
            @Param("categorySlug") String categorySlug
    );

    @Query(value = """
        SELECT 
            MIN(v.price) as min_price,
            MAX(v.price) as max_price
        FROM products p
        LEFT JOIN categories c ON p.category_id = c.id
        INNER JOIN brands b ON p.brand_id = b.id
        INNER JOIN product_variants v ON v.product_id = p.id 
            AND v.deleted_at IS NULL 
            AND v.active = true
        WHERE p.deleted_at IS NULL
        AND p.active = true
        AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:categorySlug IS NULL OR c.slug = :categorySlug)
        AND b.slug IN :brandSlugs
        """, nativeQuery = true)
    List<Object[]> findPriceRangeWithBrands(
            @Param("search") String search,
            @Param("categorySlug") String categorySlug,
            @Param("brandSlugs") List<String> brandSlugs
    );

    @Query(value = """
        SELECT COUNT(DISTINCT p.id)
        FROM products p
        LEFT JOIN categories c ON p.category_id = c.id
        LEFT JOIN brands b ON p.brand_id = b.id
        INNER JOIN product_variants v ON v.product_id = p.id 
            AND v.deleted_at IS NULL 
            AND v.active = true
        WHERE p.deleted_at IS NULL
        AND p.active = true
        AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:categorySlug IS NULL OR c.slug = :categorySlug)
        AND v.price >= :minPrice
        AND (:maxPrice IS NULL OR v.price < :maxPrice)
        """, nativeQuery = true)
    List<Object[]> countProductsInPriceRangeWithoutBrands(
            @Param("search") String search,
            @Param("categorySlug") String categorySlug,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    @Query(value = """
        SELECT COUNT(DISTINCT p.id)
        FROM products p
        LEFT JOIN categories c ON p.category_id = c.id
        INNER JOIN brands b ON p.brand_id = b.id
        INNER JOIN product_variants v ON v.product_id = p.id 
            AND v.deleted_at IS NULL 
            AND v.active = true
        WHERE p.deleted_at IS NULL
        AND p.active = true
        AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:categorySlug IS NULL OR c.slug = :categorySlug)
        AND b.slug IN :brandSlugs
        AND v.price >= :minPrice
        AND (:maxPrice IS NULL OR v.price < :maxPrice)
        """, nativeQuery = true)
    List<Object[]> countProductsInPriceRangeWithBrands(
            @Param("search") String search,
            @Param("categorySlug") String categorySlug,
            @Param("brandSlugs") List<String> brandSlugs,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    @Query(value = """
        SELECT p.id
        FROM products p
        LEFT JOIN categories c ON p.category_id = c.id
        LEFT JOIN brands b ON p.brand_id = b.id
        LEFT JOIN (
            SELECT product_id, MIN(price) as min_price
            FROM product_variants
            WHERE deleted_at IS NULL AND active = true
            GROUP BY product_id
        ) v ON v.product_id = p.id
        WHERE p.deleted_at IS NULL
        AND (:active IS NULL OR p.active = :active)
        AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:categorySlug IS NULL OR c.slug = :categorySlug)
        AND (COALESCE(:brandSlugs) IS NULL OR b.slug IN (:brandSlugs))
        AND (:minPrice IS NULL OR v.min_price >= :minPrice)
        AND (:maxPrice IS NULL OR v.min_price <= :maxPrice)
        AND (:badge IS NULL OR p.manual_badge = :badge)
        ORDER BY v.min_price ASC NULLS LAST
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Long> findProductIdsSortedByPriceAsc(
            @Param("active") Boolean active,
            @Param("search") String search,
            @Param("categorySlug") String categorySlug,
            @Param("brandSlugs") List<String> brandSlugs,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("badge") String badge,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT p.id
        FROM products p
        LEFT JOIN categories c ON p.category_id = c.id
        LEFT JOIN brands b ON p.brand_id = b.id
        LEFT JOIN (
            SELECT product_id, MIN(price) as min_price
            FROM product_variants
            WHERE deleted_at IS NULL AND active = true
            GROUP BY product_id
        ) v ON v.product_id = p.id
        WHERE p.deleted_at IS NULL
        AND (:active IS NULL OR p.active = :active)
        AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:categorySlug IS NULL OR c.slug = :categorySlug)
        AND (COALESCE(:brandSlugs) IS NULL OR b.slug IN (:brandSlugs))
        AND (:minPrice IS NULL OR v.min_price >= :minPrice)
        AND (:maxPrice IS NULL OR v.min_price <= :maxPrice)
        AND (:badge IS NULL OR p.manual_badge = :badge)
        ORDER BY v.min_price DESC NULLS LAST
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Long> findProductIdsSortedByPriceDesc(
            @Param("active") Boolean active,
            @Param("search") String search,
            @Param("categorySlug") String categorySlug,
            @Param("brandSlugs") List<String> brandSlugs,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("badge") String badge,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(DISTINCT p.id)
        FROM products p
        LEFT JOIN categories c ON p.category_id = c.id
        LEFT JOIN brands b ON p.brand_id = b.id
        LEFT JOIN product_variants v ON v.product_id = p.id 
            AND v.deleted_at IS NULL 
            AND v.active = true
        WHERE p.deleted_at IS NULL
        AND (:active IS NULL OR p.active = :active)
        AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:categorySlug IS NULL OR c.slug = :categorySlug)
        AND (COALESCE(:brandSlugs) IS NULL OR b.slug IN (:brandSlugs))
        AND (:minPrice IS NULL OR v.price >= :minPrice)
        AND (:maxPrice IS NULL OR v.price <= :maxPrice)
        AND (:badge IS NULL OR p.manual_badge = :badge)
        """, nativeQuery = true)
    long countProductsWithFilters(
            @Param("active") Boolean active,
            @Param("search") String search,
            @Param("categorySlug") String categorySlug,
            @Param("brandSlugs") List<String> brandSlugs,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("badge") String badge
    );
}
