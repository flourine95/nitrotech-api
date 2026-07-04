package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long>, JpaSpecificationExecutor<OrderEntity> {

    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id AND o.deletedAt IS NULL")
    Optional<OrderEntity> findActiveById(@Param("id") Long id);

    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id AND o.userId = :userId AND o.deletedAt IS NULL")
    Optional<OrderEntity> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
            SELECT o FROM OrderEntity o
            WHERE o.userId = :userId
            AND o.idempotencyKey = :idempotencyKey
            AND o.deletedAt IS NULL
            """)
    Optional<OrderEntity> findByUserIdAndIdempotencyKey(
            @Param("userId") Long userId,
            @Param("idempotencyKey") String idempotencyKey
    );

    @Query("SELECT i.order.id, COUNT(i.id) FROM OrderItemEntity i WHERE i.order.id IN :orderIds GROUP BY i.order.id")
    List<Object[]> countItemsForOrders(@Param("orderIds") List<Long> orderIds);

    boolean existsByIdAndUserId(Long id, Long userId);

    @Query("""
            SELECT o FROM OrderEntity o
            WHERE o.status = 'pending'
            AND o.createdAt <= :cutoff
            AND o.deletedAt IS NULL
            """)
    List<OrderEntity> findPendingCreatedAtOrBefore(@Param("cutoff") Instant cutoff);

}
