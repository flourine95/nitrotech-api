package com.nitrotech.api.domain.review.repository;

import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.dto.CreateReviewCommand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ReviewRepository {
    ReviewData create(CreateReviewCommand command);
    Optional<ReviewData> findById(Long id);
    Page<ReviewData> findByProductId(Long productId, String status, Pageable pageable);
    Page<ReviewData> findPending(Pageable pageable);
    ReviewData updateStatus(Long id, String status);
    boolean existsByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);
    void softDelete(Long id);
}
