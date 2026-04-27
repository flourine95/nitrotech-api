package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BrandJpaRepository extends JpaRepository<BrandEntity, Long>,
        JpaSpecificationExecutor<BrandEntity> {

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END FROM BrandEntity b WHERE b.slug = :slug AND b.deletedAt IS NULL")
    boolean existsActiveBySlug(@Param("slug") String slug);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END FROM BrandEntity b WHERE b.slug = :slug AND b.deletedAt IS NULL AND b.id != :excludeId")
    boolean existsActiveBySlugAndIdNot(@Param("slug") String slug, @Param("excludeId") Long excludeId);

    @Query("SELECT b FROM BrandEntity b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<BrandEntity> findActiveById(@Param("id") Long id);

    @Query("SELECT b FROM BrandEntity b WHERE b.id = :id AND b.deletedAt IS NOT NULL")
    Optional<BrandEntity> findDeletedById(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END FROM BrandEntity b WHERE b.id = :id AND b.deletedAt IS NULL")
    boolean existsActiveById(@Param("id") Long id);

    @Query("SELECT b FROM BrandEntity b WHERE b.id IN :ids AND b.deletedAt IS NULL")
    List<BrandEntity> findAllActiveByIds(@Param("ids") List<Long> ids);

    @Query("SELECT b FROM BrandEntity b WHERE b.id IN :ids AND b.deletedAt IS NOT NULL")
    List<BrandEntity> findAllDeletedByIds(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE BrandEntity b SET b.deletedAt = :now WHERE b.id IN :ids AND b.deletedAt IS NULL")
    int bulkSoftDelete(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE BrandEntity b SET b.deletedAt = NULL WHERE b.id IN :ids AND b.deletedAt IS NOT NULL")
    int bulkRestore(@Param("ids") List<Long> ids);

    @Query("SELECT b.id FROM BrandEntity b WHERE b.id IN :ids AND b.deletedAt IS NOT NULL")
    List<Long> findDeletedIdsByIds(@Param("ids") List<Long> ids);

    // Đếm 3 nhóm trong 1 query, filter theo search context
    @Query("""
            SELECT
              SUM(CASE WHEN b.deletedAt IS NULL AND b.active = true  THEN 1 ELSE 0 END),
              SUM(CASE WHEN b.deletedAt IS NULL AND b.active = false THEN 1 ELSE 0 END),
              SUM(CASE WHEN b.deletedAt IS NOT NULL                  THEN 1 ELSE 0 END)
            FROM BrandEntity b
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(b.slug) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    List<Object[]> countFacets(@Param("search") String search);
}
