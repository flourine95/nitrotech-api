package com.nitrotech.api.domain.review.usecase;

import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ModerateReviewUseCase {

    private final ReviewRepository reviewRepository;

    public ModerateReviewUseCase(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public ReviewData approve(Long id) {
        reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("REVIEW_NOT_FOUND", "Review not found"));
        return reviewRepository.updateStatus(id, "approved");
    }

    public ReviewData reject(Long id) {
        reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("REVIEW_NOT_FOUND", "Review not found"));
        return reviewRepository.updateStatus(id, "rejected");
    }
}
