package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.order.dto.OrderFilter;
import com.nitrotech.api.domain.order.dto.OrderListItemData;
import com.nitrotech.api.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id AND o.deletedAt IS NULL")
    Optional<OrderEntity> findActiveById(@Param("id") Long id);

    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id AND o.userId = :userId AND o.deletedAt IS NULL")
    Optional<OrderEntity> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
            SELECT new com.nitrotech.api.domain.order.dto.OrderListItemData(
                o.id,
                o.userId,
                function('jsonb_extract_path_text', o.shippingAddress, 'receiver'),
                function('jsonb_extract_path_text', o.shippingAddress, 'phone'),
                o.status,
                o.paymentMethod,
                o.finalAmount,
                (SELECT COUNT(i.id) FROM OrderItemEntity i WHERE i.order = o),
                o.createdAt,
                o.updatedAt
            )
            FROM OrderEntity o
            LEFT JOIN UserEntity u ON u.id = o.userId
            WHERE o.deletedAt IS NULL
            AND (:#{#filter.userId} IS NULL OR o.userId = :#{#filter.userId})
            AND (:#{#filter.status} IS NULL OR o.status = :#{#filter.status})
            AND (:#{#filter.paymentMethod} IS NULL OR o.paymentMethod = :#{#filter.paymentMethod})
            AND (:#{#filter.createdFrom} IS NULL OR o.createdAt >= :#{#filter.createdFrom})
            AND (:#{#filter.createdToExclusive} IS NULL OR o.createdAt < :#{#filter.createdToExclusive})
            AND (:#{#filter.amountMin} IS NULL OR o.finalAmount >= :#{#filter.amountMin})
            AND (:#{#filter.amountMax} IS NULL OR o.finalAmount <= :#{#filter.amountMax})
            AND (
                :#{#filter.search} IS NULL
                OR lower(str(o.id)) LIKE lower(concat('%', :#{#filter.search}, '%'))
                OR lower(function('jsonb_extract_path_text', o.shippingAddress, 'receiver')) LIKE lower(concat('%', :#{#filter.search}, '%'))
                OR lower(function('jsonb_extract_path_text', o.shippingAddress, 'phone')) LIKE lower(concat('%', :#{#filter.search}, '%'))
                OR lower(u.email) LIKE lower(concat('%', :#{#filter.search}, '%'))
            )
            """)
    Page<OrderListItemData> findList(@Param("filter") OrderFilter filter, Pageable pageable);

    @Query("""
            SELECT COUNT(o.id)
            FROM OrderEntity o
            LEFT JOIN UserEntity u ON u.id = o.userId
            WHERE o.deletedAt IS NULL
            AND (:#{#filter.userId} IS NULL OR o.userId = :#{#filter.userId})
            AND (:#{#filter.paymentMethod} IS NULL OR o.paymentMethod = :#{#filter.paymentMethod})
            AND (:#{#filter.createdFrom} IS NULL OR o.createdAt >= :#{#filter.createdFrom})
            AND (:#{#filter.createdToExclusive} IS NULL OR o.createdAt < :#{#filter.createdToExclusive})
            AND (:#{#filter.amountMin} IS NULL OR o.finalAmount >= :#{#filter.amountMin})
            AND (:#{#filter.amountMax} IS NULL OR o.finalAmount <= :#{#filter.amountMax})
            AND (
                :#{#filter.search} IS NULL
                OR lower(str(o.id)) LIKE lower(concat('%', :#{#filter.search}, '%'))
                OR lower(function('jsonb_extract_path_text', o.shippingAddress, 'receiver')) LIKE lower(concat('%', :#{#filter.search}, '%'))
                OR lower(function('jsonb_extract_path_text', o.shippingAddress, 'phone')) LIKE lower(concat('%', :#{#filter.search}, '%'))
                OR lower(u.email) LIKE lower(concat('%', :#{#filter.search}, '%'))
            )
            """)
    long countFacetsTotal(@Param("filter") OrderFilter filter);

    @Query("""
            SELECT o.status, COUNT(o.id)
            FROM OrderEntity o
            LEFT JOIN UserEntity u ON u.id = o.userId
            WHERE o.deletedAt IS NULL
            AND (:#{#filter.userId} IS NULL OR o.userId = :#{#filter.userId})
            AND (:#{#filter.paymentMethod} IS NULL OR o.paymentMethod = :#{#filter.paymentMethod})
            AND (:#{#filter.createdFrom} IS NULL OR o.createdAt >= :#{#filter.createdFrom})
            AND (:#{#filter.createdToExclusive} IS NULL OR o.createdAt < :#{#filter.createdToExclusive})
            AND (:#{#filter.amountMin} IS NULL OR o.finalAmount >= :#{#filter.amountMin})
            AND (:#{#filter.amountMax} IS NULL OR o.finalAmount <= :#{#filter.amountMax})
            AND (
                :#{#filter.search} IS NULL
                OR lower(str(o.id)) LIKE lower(concat('%', :#{#filter.search}, '%'))
                OR lower(function('jsonb_extract_path_text', o.shippingAddress, 'receiver')) LIKE lower(concat('%', :#{#filter.search}, '%'))
                OR lower(function('jsonb_extract_path_text', o.shippingAddress, 'phone')) LIKE lower(concat('%', :#{#filter.search}, '%'))
                OR lower(u.email) LIKE lower(concat('%', :#{#filter.search}, '%'))
            )
            GROUP BY o.status
            """)
    List<Object[]> countStatusFacets(@Param("filter") OrderFilter filter);

    @Query("""
            SELECT o.paymentMethod, COUNT(o.id)
            FROM OrderEntity o
            LEFT JOIN UserEntity u ON u.id = o.userId
            WHERE o.deletedAt IS NULL
            AND (:#{#filter.userId} IS NULL OR o.userId = :#{#filter.userId})
            AND (:#{#filter.status} IS NULL OR o.status = :#{#filter.status})
            AND (:#{#filter.createdFrom} IS NULL OR o.createdAt >= :#{#filter.createdFrom})
            AND (:#{#filter.createdToExclusive} IS NULL OR o.createdAt < :#{#filter.createdToExclusive})
            AND (:#{#filter.amountMin} IS NULL OR o.finalAmount >= :#{#filter.amountMin})
            AND (:#{#filter.amountMax} IS NULL OR o.finalAmount <= :#{#filter.amountMax})
            AND (
                :#{#filter.search} IS NULL
                OR lower(str(o.id)) LIKE lower(concat('%', :#{#filter.search}, '%'))
                OR lower(function('jsonb_extract_path_text', o.shippingAddress, 'receiver')) LIKE lower(concat('%', :#{#filter.search}, '%'))
                OR lower(function('jsonb_extract_path_text', o.shippingAddress, 'phone')) LIKE lower(concat('%', :#{#filter.search}, '%'))
                OR lower(u.email) LIKE lower(concat('%', :#{#filter.search}, '%'))
            )
            GROUP BY o.paymentMethod
            """)
    List<Object[]> countPaymentMethodFacets(@Param("filter") OrderFilter filter);

    boolean existsByIdAndUserId(Long id, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE orders
            SET status = 'expired'
            , updated_at = :expiredAt
            WHERE status = 'pending'
            AND created_at <= :cutoff
            AND deleted_at IS NULL
            """, nativeQuery = true)
    int expirePendingCreatedAtOrBefore(
            @Param("cutoff") Instant cutoff,
            @Param("expiredAt") Instant expiredAt
    );
}
