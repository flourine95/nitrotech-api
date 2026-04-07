package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long>,
        JpaSpecificationExecutor<CategoryEntity> {

    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query("SELECT c FROM CategoryEntity c WHERE c.deletedAt IS NULL AND (:active IS NULL OR c.active = :active) AND (:parentId IS NULL OR c.parentId = :parentId) ORDER BY c.name")
    List<CategoryEntity> findAllActive(@Param("active") Boolean active, @Param("parentId") Long parentId);

    @Query("SELECT c FROM CategoryEntity c WHERE c.deletedAt IS NULL AND (:active IS NULL OR c.active = :active) ORDER BY c.parentId NULLS FIRST, c.name")
    List<CategoryEntity> findAllForTree(@Param("active") Boolean active);

    @Query("SELECT c FROM CategoryEntity c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<CategoryEntity> findActiveById(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM CategoryEntity c WHERE c.id = :id AND c.deletedAt IS NULL")
    boolean existsActiveById(@Param("id") Long id);
}
