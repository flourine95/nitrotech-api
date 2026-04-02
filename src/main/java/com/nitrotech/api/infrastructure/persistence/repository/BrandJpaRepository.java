package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BrandJpaRepository extends JpaRepository<BrandEntity, Long> {

    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query("SELECT b FROM BrandEntity b WHERE b.deletedAt IS NULL AND (:active IS NULL OR b.active = :active) ORDER BY b.name")
    List<BrandEntity> findAllActive(@Param("active") Boolean active);

    @Query("SELECT b FROM BrandEntity b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<BrandEntity> findActiveById(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END FROM BrandEntity b WHERE b.id = :id AND b.deletedAt IS NULL")
    boolean existsActiveById(@Param("id") Long id);
}
