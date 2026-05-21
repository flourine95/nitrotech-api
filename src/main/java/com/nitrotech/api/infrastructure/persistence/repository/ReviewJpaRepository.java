package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, Long> {

    /**
     * Get review statistics for a single product
     * Returns: [avgRating (Double), reviewCount (Long)]
     */
    @Query("""
        SELECT COALESCE(AVG(r.rating), 0.0), COUNT(r.id)
        FROM ReviewEntity r
        WHERE r.productId = :productId 
          AND r.status = 'approved' 
          AND r.deletedAt IS NULL
        """)
    Object[] getReviewStats(@Param("productId") Long productId);

    /**
     * Batch get review statistics for multiple products
     * 
     * @param productIds List of product IDs to fetch stats for
     * @return List of Object[] where each array contains:
     *         [0] Long productId
     *         [1] Double avgRating (null if no reviews)
     *         [2] Long reviewCount
     */
    @Query("""
        SELECT r.productId, AVG(r.rating), COUNT(r.id)
        FROM ReviewEntity r
        WHERE r.productId IN :productIds 
          AND r.status = 'approved' 
          AND r.deletedAt IS NULL
        GROUP BY r.productId
        """)
    List<Object[]> getReviewStatsBatch(@Param("productIds") List<Long> productIds);

    // Query methods for ReviewRepositoryImpl

    @Query("SELECT r FROM ReviewEntity r WHERE r.id = :id AND r.deletedAt IS NULL")
    Optional<ReviewEntity> findActiveById(@Param("id") Long id);

    @Query("""
        SELECT r FROM ReviewEntity r 
        WHERE r.productId = :productId 
          AND (:status IS NULL OR r.status = :status)
          AND r.deletedAt IS NULL
        ORDER BY r.createdAt DESC
        """)
    Page<ReviewEntity> findByProductId(
            @Param("productId") Long productId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
        SELECT r FROM ReviewEntity r 
        WHERE r.status = 'pending' 
          AND r.deletedAt IS NULL
        ORDER BY r.createdAt ASC
        """)
    Page<ReviewEntity> findPending(Pageable pageable);

    boolean existsByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);
}
