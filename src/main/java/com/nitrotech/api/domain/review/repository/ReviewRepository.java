package com.nitrotech.api.domain.review.repository;

import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.dto.CreateReviewCommand;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository {
    ReviewData create(CreateReviewCommand command);
    Optional<ReviewData> findById(Long id);
    List<ReviewData> findByProductId(Long productId, String status, int page, int size);
    long countByProductId(Long productId, String status);
    List<ReviewData> findPending(int page, int size);
    long countPending();
    ReviewData updateStatus(Long id, String status);
    boolean existsByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);
    void softDelete(Long id);
}
