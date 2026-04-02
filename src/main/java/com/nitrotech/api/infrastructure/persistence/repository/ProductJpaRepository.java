package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL AND p.id = :id")
    Optional<ProductEntity> findActiveById(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END FROM ProductEntity p WHERE p.id = :id AND p.deletedAt IS NULL")
    boolean existsActiveById(@Param("id") Long id);

    @Query("""
            SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL
            AND (:categoryId IS NULL OR p.categoryId = :categoryId)
            AND (:brandId IS NULL OR p.brandId = :brandId)
            AND (:active IS NULL OR p.active = :active)
            AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY p.createdAt DESC
            """)
    Page<ProductEntity> findAllFiltered(
            @Param("categoryId") Long categoryId,
            @Param("brandId") Long brandId,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );
}
