package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.ReviewReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewReportJpaRepository extends JpaRepository<ReviewReportEntity, Long> {
    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);
}
