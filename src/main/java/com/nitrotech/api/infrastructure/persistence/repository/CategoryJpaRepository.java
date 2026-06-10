package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long>,
        JpaSpecificationExecutor<CategoryEntity> {

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM CategoryEntity c WHERE c.slug = :slug AND c.deletedAt IS NULL")
    boolean existsNotDeletedBySlug(@Param("slug") String slug);
    @Query("SELECT c FROM CategoryEntity c WHERE c.deletedAt IS NULL AND (:active IS NULL OR c.active = :active) AND ((:parentId IS NULL AND c.parentId IS NULL) OR c.parentId = :parentId) ORDER BY c.sortOrder ASC, c.id ASC")
    List<CategoryEntity> findAllNotDeleted(@Param("active") Boolean active, @Param("parentId") Long parentId);

    @Query("SELECT c FROM CategoryEntity c WHERE c.deletedAt IS NULL AND (:active IS NULL OR c.active = :active) ORDER BY c.parentId NULLS FIRST, c.sortOrder ASC, c.name ASC")
    List<CategoryEntity> findAllForTree(@Param("active") Boolean active);

    @Query("SELECT c FROM CategoryEntity c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<CategoryEntity> findNotDeletedById(@Param("id") Long id);

    Optional<CategoryEntity> findBySlugAndDeletedAtIsNull(String slug);

    @Query("SELECT c FROM CategoryEntity c WHERE c.id = :id AND c.active = true AND c.deletedAt IS NULL")
    Optional<CategoryEntity> findVisibleById(@Param("id") Long id);

    Optional<CategoryEntity> findBySlugAndActiveTrueAndDeletedAtIsNull(String slug);

    @Query(value = """
        WITH RECURSIVE category_tree AS (
            SELECT id
            FROM categories
            WHERE slug = :slug AND deleted_at IS NULL
            UNION ALL
            SELECT c.id
            FROM categories c
            INNER JOIN category_tree ct ON c.parent_id = ct.id
            WHERE c.deleted_at IS NULL
        )
        SELECT id FROM category_tree
        """, nativeQuery = true)
    List<Long> findDescendantIdsBySlug(@Param("slug") String slug);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM CategoryEntity c WHERE c.id = :id AND c.deletedAt IS NULL")
    boolean existsNotDeletedById(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM CategoryEntity c WHERE c.parentId = :parentId AND c.deletedAt IS NULL")
    boolean existsNotDeletedChildrenByParentId(@Param("parentId") Long parentId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM CategoryEntity c WHERE c.parentId = :parentId")
    boolean existsAnyChildrenByParentId(@Param("parentId") Long parentId);

    @Query("SELECT c FROM CategoryEntity c WHERE c.id = :id AND c.deletedAt IS NOT NULL")
    Optional<CategoryEntity> findDeletedById(@Param("id") Long id);

    @Query("SELECT c FROM CategoryEntity c WHERE c.deletedAt IS NOT NULL ORDER BY c.deletedAt DESC")
    List<CategoryEntity> findAllDeleted();

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM CategoryEntity c WHERE c.slug = :slug AND c.deletedAt IS NULL AND c.id != :excludeId")
    boolean existsNotDeletedBySlugAndIdNot(@Param("slug") String slug, @Param("excludeId") Long excludeId);
    
    // Bulk operations
    @Query("SELECT c FROM CategoryEntity c WHERE c.id IN :ids AND c.deletedAt IS NULL")
    List<CategoryEntity> findAllNotDeletedByIds(@Param("ids") List<Long> ids);
    
    @Query("SELECT c FROM CategoryEntity c WHERE c.id IN :ids AND c.deletedAt IS NOT NULL")
    List<CategoryEntity> findAllDeletedByIds(@Param("ids") List<Long> ids);
    
    @Modifying
    @Query("UPDATE CategoryEntity c SET c.deletedAt = :now WHERE c.id IN :ids AND c.deletedAt IS NULL")
    int bulkSoftDelete(@Param("ids") List<Long> ids, @Param("now") Instant now);
    
    @Modifying
    @Query("UPDATE CategoryEntity c SET c.deletedAt = NULL WHERE c.id IN :ids AND c.deletedAt IS NOT NULL")
    int bulkRestore(@Param("ids") List<Long> ids);
    
    @Modifying
    @Query("UPDATE CategoryEntity c SET c.active = true WHERE c.id IN :ids AND c.deletedAt IS NULL")
    int bulkActivate(@Param("ids") List<Long> ids);
    
    @Modifying
    @Query("UPDATE CategoryEntity c SET c.active = false WHERE c.id IN :ids AND c.deletedAt IS NULL")
    int bulkDeactivate(@Param("ids") List<Long> ids);
    
    /**
     * Get breadcrumb path for a category (recursive parent lookup)
     * Returns: [id (Long), name (String), slug (String), active (Boolean)]
     */
    @Query(value = """
        WITH RECURSIVE category_path AS (
            SELECT id, name, slug, active, parent_id, 0 as depth
            FROM categories
            WHERE id = :categoryId
            UNION ALL
            SELECT c.id, c.name, c.slug, c.active, c.parent_id, cp.depth + 1
            FROM categories c
            INNER JOIN category_path cp ON c.id = cp.parent_id
        )
        SELECT id, name, slug, active
        FROM category_path
        WHERE id != :categoryId
        ORDER BY depth DESC
        """, nativeQuery = true)
    List<Object[]> findPath(@Param("categoryId") Long categoryId);
    
    /**
     * Get category statistics in one query
     * Returns: [activeCount (Long), inactiveCount (Long), deletedCount (Long), rootCount (Long), withChildrenCount (Long)]
     */
    @Query(value = """
        SELECT
            COUNT(*) FILTER (WHERE active = true AND deleted_at IS NULL) as active,
            COUNT(*) FILTER (WHERE active = false AND deleted_at IS NULL) as inactive,
            COUNT(*) FILTER (WHERE deleted_at IS NOT NULL) as deleted,
            COUNT(*) FILTER (WHERE parent_id IS NULL AND deleted_at IS NULL) as root,
            COUNT(DISTINCT CASE WHEN id IN (SELECT DISTINCT parent_id FROM categories WHERE parent_id IS NOT NULL AND deleted_at IS NULL) THEN id END) as with_children
        FROM categories
        """, nativeQuery = true)
    List<Object[]> getFacets();
    
    /**
     * Batch get product counts for all categories
     * Returns: [categoryId (Long), productCount (Long)]
     */
    @Query(value = """
        SELECT c.id, COUNT(p.id)
        FROM categories c
        LEFT JOIN products p ON p.category_id = c.id AND p.deleted_at IS NULL
        WHERE c.deleted_at IS NULL
        GROUP BY c.id
        """, nativeQuery = true)
    List<Object[]> getProductCountsForAllCategories();
    
    // Count products for a single category (only active products)
    @Query("SELECT COUNT(p) FROM ProductEntity p WHERE p.categoryId = :categoryId AND p.deletedAt IS NULL")
    int countProductsByCategoryId(@Param("categoryId") Long categoryId);
}
