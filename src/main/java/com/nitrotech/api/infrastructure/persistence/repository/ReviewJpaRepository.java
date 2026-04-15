package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, Long> {

    boolean existsByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.id = :id AND r.deletedAt IS NULL")
    Optional<ReviewEntity> findActiveById(@Param("id") Long id);

    @Query("""
            SELECT r FROM ReviewEntity r WHERE r.deletedAt IS NULL
            AND r.productId = :productId
            AND (:status IS NULL OR r.status = :status)
            ORDER BY r.createdAt DESC
            """)
    Page<ReviewEntity> findByProductId(@Param("productId") Long productId,
                                        @Param("status") String status, Pageable pageable);

    @Query("SELECT r FROM ReviewEntity r WHERE r.deletedAt IS NULL AND r.status = 'pending' ORDER BY r.createdAt ASC")
    Page<ReviewEntity> findPending(Pageable pageable);
}
