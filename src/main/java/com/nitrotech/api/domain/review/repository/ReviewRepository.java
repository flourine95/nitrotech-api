package com.nitrotech.api.domain.review.repository;

import com.nitrotech.api.domain.review.ReviewStatus;
import com.nitrotech.api.domain.review.dto.CreateReviewCommand;
import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.dto.ReviewStatsData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ReviewRepository {
    ReviewData create(CreateReviewCommand command);

    Optional<ReviewData> findById(Long id);

    Optional<ReviewData> findByIdAndUserId(Long id, Long userId);

    Page<ReviewData> findByProductId(Long productId, String status, Pageable pageable);

    Page<ReviewData> findAll(String status, Pageable pageable);

    Page<ReviewData> findPending(Pageable pageable);

    ReviewStatsData getStats(Long productId);

    ReviewData updateStatus(Long id, ReviewStatus status);

    ReviewData update(Long id, int rating, String comment, java.util.List<String> images);

    boolean existsByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);

    boolean orderContainsProduct(Long orderId, Long productId);

    boolean reportExists(Long reviewId, Long userId);

    void report(Long reviewId, Long userId, String reason);

    void softDelete(Long id);
}
