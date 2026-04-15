package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
