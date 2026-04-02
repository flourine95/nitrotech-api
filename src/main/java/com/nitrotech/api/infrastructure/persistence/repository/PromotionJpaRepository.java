package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.PromotionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PromotionJpaRepository extends JpaRepository<PromotionEntity, Long> {

    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);

    @Query("""
            SELECT p FROM PromotionEntity p WHERE p.code = :code AND p.status = 'active'
            AND p.startAt <= :now AND p.endAt >= :now
            """)
    Optional<PromotionEntity> findActiveByCode(@Param("code") String code, @Param("now") LocalDateTime now);

    @Query("SELECT p FROM PromotionEntity p WHERE (:status IS NULL OR p.status = :status) ORDER BY p.priority DESC, p.createdAt DESC")
    Page<PromotionEntity> findAllFiltered(@Param("status") String status, Pageable pageable);
}
