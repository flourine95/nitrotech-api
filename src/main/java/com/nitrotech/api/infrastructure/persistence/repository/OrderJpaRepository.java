package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id AND o.deletedAt IS NULL")
    Optional<OrderEntity> findActiveById(@Param("id") Long id);

    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id AND o.userId = :userId AND o.deletedAt IS NULL")
    Optional<OrderEntity> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
            SELECT o FROM OrderEntity o WHERE o.deletedAt IS NULL
            AND (:userId IS NULL OR o.userId = :userId)
            AND (:status IS NULL OR o.status = :status)
            ORDER BY o.createdAt DESC
            """)
    Page<OrderEntity> findAllFiltered(
            @Param("userId") Long userId,
            @Param("status") String status,
            Pageable pageable
    );

    boolean existsByIdAndUserId(Long id, Long userId);
}
