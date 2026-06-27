package com.nitrotech.api.domain.review.usecase;

import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.exception.ReviewNotFoundException;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import org.springframework.stereotype.Service;

@Service
public class ModerateReviewUseCase {

    private final ReviewRepository reviewRepository;

    public ModerateReviewUseCase(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public ReviewData approve(Long id) {
        reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException());
        return reviewRepository.updateStatus(id, "approved");
    }

    public ReviewData reject(Long id) {
        reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException());
        return reviewRepository.updateStatus(id, "rejected");
    }
}
