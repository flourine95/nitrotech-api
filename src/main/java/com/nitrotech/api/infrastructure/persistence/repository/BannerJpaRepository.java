package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.BannerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BannerJpaRepository extends JpaRepository<BannerEntity, Long> {

    @Query("""
            SELECT b FROM BannerEntity b WHERE b.active = TRUE
            AND (:position IS NULL OR b.position = :position)
            AND (b.startDate IS NULL OR b.startDate <= :now)
            AND (b.endDate IS NULL OR b.endDate >= :now)
            ORDER BY b.sortOrder ASC
            """)
    List<BannerEntity> findActive(@Param("position") String position, @Param("now") LocalDateTime now);

    @Query("""
            SELECT b FROM BannerEntity b
            WHERE (:active IS NULL OR b.active = :active)
            AND (:position IS NULL OR b.position = :position)
            ORDER BY b.sortOrder ASC, b.createdAt DESC
            """)
    List<BannerEntity> findAllFiltered(@Param("active") Boolean active, @Param("position") String position);
}
