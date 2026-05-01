package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long>,
        JpaSpecificationExecutor<CategoryEntity> {

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM CategoryEntity c WHERE c.slug = :slug AND c.deletedAt IS NULL")
    boolean existsActiveBySlug(@Param("slug") String slug);
    @Query("SELECT c FROM CategoryEntity c WHERE c.deletedAt IS NULL AND (:active IS NULL OR c.active = :active) AND (:parentId IS NULL OR c.parentId = :parentId) ORDER BY c.name")
    List<CategoryEntity> findAllActive(@Param("active") Boolean active, @Param("parentId") Long parentId);

    @Query("SELECT c FROM CategoryEntity c WHERE c.deletedAt IS NULL AND (:active IS NULL OR c.active = :active) ORDER BY c.parentId NULLS FIRST, c.sortOrder ASC, c.name ASC")
    List<CategoryEntity> findAllForTree(@Param("active") Boolean active);

    @Query("SELECT c FROM CategoryEntity c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<CategoryEntity> findActiveById(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM CategoryEntity c WHERE c.id = :id AND c.deletedAt IS NULL")
    boolean existsActiveById(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM CategoryEntity c WHERE c.parentId = :parentId AND c.deletedAt IS NULL")
    boolean existsActiveChildrenByParentId(@Param("parentId") Long parentId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM CategoryEntity c WHERE c.parentId = :parentId")
    boolean existsAnyChildrenByParentId(@Param("parentId") Long parentId);

    @Query("SELECT c FROM CategoryEntity c WHERE c.id = :id AND c.deletedAt IS NOT NULL")
    Optional<CategoryEntity> findDeletedById(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM CategoryEntity c WHERE c.slug = :slug AND c.deletedAt IS NULL AND c.id != :excludeId")
    boolean existsActiveBySlugAndIdNot(@Param("slug") String slug, @Param("excludeId") Long excludeId);
    
    // Bulk operations
    @Query("SELECT c FROM CategoryEntity c WHERE c.id IN :ids AND c.deletedAt IS NULL")
    List<CategoryEntity> findAllActiveByIds(@Param("ids") List<Long> ids);
    
    @Query("SELECT c FROM CategoryEntity c WHERE c.id IN :ids AND c.deletedAt IS NOT NULL")
    List<CategoryEntity> findAllDeletedByIds(@Param("ids") List<Long> ids);
    
    @Modifying
    @Query("UPDATE CategoryEntity c SET c.deletedAt = :now WHERE c.id IN :ids AND c.deletedAt IS NULL")
    int bulkSoftDelete(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE CategoryEntity c SET c.deletedAt = NULL WHERE c.id IN :ids AND c.deletedAt IS NOT NULL")
    int bulkRestore(@Param("ids") List<Long> ids);
    
    @Modifying
    @Query("UPDATE CategoryEntity c SET c.active = true WHERE c.id IN :ids AND c.deletedAt IS NULL")
    int bulkActivate(@Param("ids") List<Long> ids);
    
    @Modifying
    @Query("UPDATE CategoryEntity c SET c.active = false WHERE c.id IN :ids AND c.deletedAt IS NULL")
    int bulkDeactivate(@Param("ids") List<Long> ids);
    
    // Breadcrumb query
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
}
